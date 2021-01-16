package cz.vse.miraql.controller;

import cz.vse.miraql.config.MiraqlConfig;
import cz.vse.miraql.model.RequestQuery;
import cz.vse.miraql.utils.SshTransportConfigCallback;
import cz.vse.miraql.utils.Util;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.jena.update.UpdateAction;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.Ontology;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.LineNumberReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Slf4j
@CrossOrigin
@Validated
public class MiraqlControllerKb {

    private final List<String> GIT_REVIEWERS = Arrays.asList("patkom", "bjoand1", "danbjo", "micska", "marfuc", "kriek", "miktor", "ricsna", "jonhag");
    private final MiraqlConfig owlManagerConfig;
    private final String NAMESPACE = "master";
    private final String API_PREFIX = "/" + NAMESPACE;
    private final String GIT_SERVICE_PREFIX = "https://github.com/stash/projects/PROJECT/repos";
    private final String GIT_REPO_NAME = "yugioh-ontology";
    private final String GIT_REPO_LOCAL_PATH = File.separator + GIT_REPO_NAME;
    private final String RDF_DATATYPE_STRING = "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\"";
    private final String RDF_DATATYPE_BYTE = "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#byte\"";
    private final String GIT_MAIN_BRANCH = "develop";
    private final String owlImportCore = "<owl:imports rdf:resource=\"http://ontology.vse.cz/core\"/>";

    private final Map<String, String> iriFileMap = getIriFileMap();
    private final Map<String, Map<String, String>> externalLibraries = new HashMap<String, Map<String, String>>(){{
        put("http://purl.org/dc/terms", new HashMap<String, String>(){{
            put("url", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_terms.rdf");
            put("file", "dcterms.rdf");
            put("content", "");
        }});
        put("http://purl.org/dc/elements/1.1", new HashMap<String, String>(){{
            put("url", "https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_elements.rdf");
            put("file", "dcelements.rdf");
            put("content", "");
        }});
        put("http://www.w3.org/2004/02/skos/core", new HashMap<String, String>(){{
            put("url", "http://www.w3.org/TR/skos-reference/skos.rdf");
            put("file", "skos.rdf");
            put("content", "");
        }});
    }};
    private final OWLOntologyLoaderConfiguration ontologyLoaderConfiguration =
            new OWLOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

    private final OWLOntologyWriterConfiguration ontologyWriterConfiguration = new OWLOntologyWriterConfiguration();

    @Autowired
    public MiraqlControllerKb(@NonNull MiraqlConfig owlManagerConfig) {
        this.owlManagerConfig = owlManagerConfig;
        externalLibraries.keySet().forEach(iri -> {
            log.info("Downloading external ontology lib " + iri);
            Map<String, String> externalLibrary = externalLibraries.get(iri);
            String url = externalLibrary.get("url");
            try {
                String fetchedOntologyText = Util.fetchLibraryTextResponse(url);
                if (!fetchedOntologyText.contains("<owl:Ontology rdf:about=\"" + iri + "\">")) {
                    fetchedOntologyText = fetchedOntologyText.replace("</rdf:RDF>", "<owl:Ontology rdf:about=\"" + iri + "\"></owl:Ontology></rdf:RDF>");
                }
                externalLibrary.put("content", fetchedOntologyText);
                log.info("Downloaded external ontology library " + iri);
            } catch (Exception e) {
                log.error(e.toString());
            }
            if (externalLibrary.get("content").isEmpty()) {
                try {
                    String fallbackOntologyText = Util.getFileContent(GIT_REPO_LOCAL_PATH + File.separator + "files" + File.separator + externalLibrary.get("file"));
                    externalLibrary.put("content", fallbackOntologyText);
                    log.info("Loaded fallback external ontology library from resource file " + externalLibrary.get("file"));
                } catch (Exception e) {
                    log.error(e.toString());
                }
            }
        });
    }

    @GetMapping(path = API_PREFIX + "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> index() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        return new ResponseEntity<>(new Gson().toJson(resp), HttpStatus.OK);
    }

    @PostMapping(path = API_PREFIX + "/test-timeout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> postTest(final OAuth2Authentication oAuth2Authentication,
                                           @RequestParam(value = "timeout") final int timeout) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return new ResponseEntity<>(new Gson().toJson(resp), HttpStatus.OK);
    }

    @GetMapping(path = API_PREFIX + "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> test() {
        OntologyManager om = OntManagers.createONT();
        externalLibraries.keySet().forEach(iri -> {
            try {
                String ontologyText = externalLibraries.get(iri).get("content");
                Ontology externalOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(ontologyText));
                log.info("Loaded ontology " + externalOntology.getOntologyID().getOntologyIRI());
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("files", iriFileMap);
        String response = new Gson().toJson(resp);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = API_PREFIX + "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> update(final OAuth2Authentication oAuth2Authentication,
                                         @RequestParam(value = "queries") final String qs,
                                         @RequestParam(value = "userEmailAddress")  final String userEmailAddress,
                                         @RequestParam(value = "changeDescription") final String changeDescription) {

        List<RequestQuery> queries = Arrays.asList(new Gson().fromJson(qs, RequestQuery[].class));
        TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback();
        String requestUUID = Util.getShortUUID();
        String newBranchName = "MIRAQL_EDIT_" + requestUUID;
        log.info(requestUUID + " | Preparing repository");
        Git git = prepareGitRepository(transportConfigCallback, requestUUID);
        try {
            if (git == null) {
                throw new Exception("Could not establish ontology git folder");
            }
            log.info(requestUUID + " | Checking git credentials ");
            git.push().setTransportConfigCallback(transportConfigCallback).call();
            log.info(requestUUID + " |   Credentials OK ");

            String ontologyGitFolder = git.getRepository().getDirectory().toString()
                    .replaceAll("\\\\\\.git$", "")
                    .replaceAll("/\\.git$", "");
            String ontologyFileDirectoryPath = ontologyGitFolder + File.separator + "files" + File.separator;
            log.info(requestUUID + " | Having ontology file directory path: " + ontologyFileDirectoryPath);

            OntologyManager om = OntManagers.createONT();
            om.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);
            log.info(requestUUID + " | Loading external ontology libs to ONT manager");
            externalLibraries.keySet().forEach(iri -> {
                try {
                    String fetchedOntologyText = externalLibraries.get(iri).get("content");
                    Ontology externalOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(fetchedOntologyText));
                    log.info(requestUUID + " |   Loaded external ontology lib to ONT manager " + externalOntology.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });

            OWLOntologyManager oom = OntManagers.createOWL();
            oom.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);
            oom.setOntologyWriterConfiguration(ontologyWriterConfiguration);
            log.info(requestUUID + " | Loading external ontology libs to OWL manager");
            Set<String> loadedOntologyIrisInOwlManager = new HashSet<>();
            externalLibraries.keySet().forEach(iri -> {
                try {
                    String fetchedOntologyText = externalLibraries.get(iri).get("content");
                    OWLOntology oo = oom.loadOntologyFromOntologyDocument(new StringDocumentSource(fetchedOntologyText));
                    loadedOntologyIrisInOwlManager.add(iri);
                    log.info(requestUUID + " |   Loaded external ontology to OWL manager " + oo.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });

            queries.forEach((query) -> {
                log.info(requestUUID + " | =========================================================");
                String targetOntologyIri = query.getTargetOntologyIri();
                String sparql = query.getSparql();
                log.info(requestUUID + " | Applying query on " + targetOntologyIri);
                String targetFileName = iriFileMap.get(targetOntologyIri);
                try {
                    String ontologyTextToUpdate = Util.getFileContent(ontologyFileDirectoryPath + targetFileName);
                    boolean addedCoreImport = false;
                    if (!ontologyTextToUpdate.contains(owlImportCore)) {
                        addedCoreImport = true;
                        ontologyTextToUpdate = ontologyTextToUpdate.replace("</owl:Ontology>", owlImportCore + "</owl:Ontology>");
                    }
                    ontologyTextToUpdate = ontologyTextToUpdate.replaceAll("<owl:AnnotationProperty rdf:about=.*/>", "");

                    log.info(requestUUID + " | Loading local import ontologies to ONT manager");
                    Util.getImportIriMatches(ontologyTextToUpdate).forEach(iri -> {
                        String fileName = ontologyFileDirectoryPath + iriFileMap.get(iri);
                        File file = new File(fileName);
                        if (!file.isFile()) {
                            return;
                        }
                        if (om.contains(IRI.create(iri))) {
                            return;
                        }
                        try {
                            String ontologyText = Util.getFileContent(fileName).replaceAll("<owl:imports.*>", "");
                            Ontology localImportOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(ontologyText));
                            log.info(requestUUID + " |   Loaded local import ontology to ONT manager " + localImportOntology.getOntologyID().getOntologyIRI());
                        } catch (Exception e) {
                            log.error(e.toString());
                        }
                    });

                    log.info(requestUUID + " | Loading target ontology to ONT manager");
                    Ontology ontology = replaceReloadOntology(om, ontologyTextToUpdate, requestUUID);

                    // ================== do the sparql update in the specified ontology (use the map) =======================
                    UpdateAction.parseExecute(sparql, ontology.asGraphModel());
                    log.info(requestUUID + " | Executed SPARQL update: " + sparql);

                    // ================================= get the updated ontology string =====================================
                    String updatedOntologyText = Util.getOntologySourceString(ontology);
                    if (addedCoreImport) {
                        updatedOntologyText = updatedOntologyText.replace(owlImportCore, "");
                    }
                    // ======= use the iris in owl:imports to import these ontologies ========================================
                    Set<String> relatedOntologyIris = getRelatedOntologyIris(updatedOntologyText, ontologyFileDirectoryPath, loadedOntologyIrisInOwlManager);
                    log.info(requestUUID + " | Loading imported ontologies (" + relatedOntologyIris.size() + ") to OWL manager");
                    for(String relatedOntologyIri : relatedOntologyIris) {
                        String importFileName = iriFileMap.get(relatedOntologyIri);
                        try {
                            String importOntologyFileText = Util.getFileContent(ontologyFileDirectoryPath + importFileName);
                            oom.loadOntologyFromOntologyDocument(new StringDocumentSource(importOntologyFileText));
                            loadedOntologyIrisInOwlManager.add(relatedOntologyIri);
                            log.info(requestUUID + " |   Loaded imported ontology to OWL manager " + importFileName + " " + relatedOntologyIri);
                        } catch (Exception e) {
                            log.error(e.getMessage());
                        }
                    }

                    log.info(requestUUID + " | Loading target ontology to OWL manager");
                    OWLOntology owlOntology = replaceReloadOntology(oom, updatedOntologyText, requestUUID);
//                    log.info(requestUUID + " | OWLOntology format: " + owlOntology.getFormat());
                    log.info(requestUUID + " | Loaded modified file to OWL manager " + targetFileName + " " + owlOntology.getOntologyID().getOntologyIRI());
//                    log.info(requestUUID + " | List of opened ontologies: ");
//                    oom.ontologies().forEach(ont -> log.info(requestUUID + " |   " + ont.getOntologyID().getOntologyIRI().toString()));
                    saveOntologyToFile(owlOntology, ontologyFileDirectoryPath + targetFileName, addedCoreImport);
                    log.info(requestUUID + " | Saved modified file " + targetFileName + " " + owlOntology.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            });

            // ================================= create branch, add changes, commit and push =========================
            log.info(requestUUID + " | Creating branch " + newBranchName);
            git.checkout().setCreateBranch(true).setName(newBranchName).call();
            log.info(requestUUID + " | Adding and committing changes: " + changeDescription);
            git.add().addFilepattern(".").call();
            git.commit().setMessage(changeDescription).setCommitter(userEmailAddress, userEmailAddress).setAuthor(userEmailAddress, userEmailAddress).call();
            log.info(requestUUID + " | Pushing to remote");
            git.push().setTransportConfigCallback(transportConfigCallback).call();
            git.checkout().setName(GIT_MAIN_BRANCH).call();
            log.info(requestUUID + " | Deleting local branch " + newBranchName);
            git.branchDelete().setBranchNames(newBranchName).setForce(true).call();
            log.info(requestUUID + " | Closing git folder");
            git.close();
            log.info(requestUUID + " | Deleting temporary git folder " + ontologyGitFolder);
            FileUtils.deleteDirectory(new File(ontologyGitFolder));

            log.info(requestUUID + " | Creating Pull Request");
            String pullRequestCreationResponse = Util.createPullRequest(GIT_SERVICE_PREFIX, GIT_REPO_NAME, newBranchName, changeDescription, GIT_REVIEWERS);
            log.info(requestUUID + " | Done :)");
            return new ResponseEntity<>(pullRequestCreationResponse, HttpStatus.OK);
        }
        catch (Exception e) {
            if (null != git) {
                git.close();
            }
            log.error("Failed to ", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            Map<String, String> resp = new HashMap<>();
            resp.put("stacktrace", sw.toString().replaceAll("\"", "\\\""));
            resp.put("message", e.getMessage());
            return new ResponseEntity<>(new Gson().toJson(resp), e.getMessage().contains("not authorized") ? HttpStatus.UNAUTHORIZED : HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private OWLOntology replaceReloadOntology(OWLOntologyManager oom, String ontologyText, String requestUUID) throws OWLOntologyCreationException {
        IRI iri = IRI.create(getOntologyIRI(ontologyText));
        if (oom.contains(iri)) {
            log.info(requestUUID + " |   Removing existing target ontology " + oom.getOntology(iri).getOntologyID() + " from OWL manager");
            oom.removeOntology(oom.getOntology(iri).getOntologyID());
        }
        StringDocumentSource sds = new StringDocumentSource(ontologyText);
        OWLOntology oo = oom.loadOntologyFromOntologyDocument(sds);
        log.info(requestUUID + " |   Loaded target ontology " + oom.getOntology(iri).getOntologyID() + " to OWL manager");
        return oo;
    }

    private Ontology replaceReloadOntology(OntologyManager om, String ontologyText, String requestUUID) throws OWLOntologyCreationException {
        IRI iri = IRI.create(getOntologyIRI(ontologyText));
        if (om.contains(iri)) {
            log.info(requestUUID + " |   Removing existing target ontology " + om.getOntology(iri).getOntologyID() + " from ONT manager");
            om.removeOntology(om.getOntology(iri).getOntologyID());
        }
        StringDocumentSource sds = new StringDocumentSource(ontologyText.replaceAll(RDF_DATATYPE_STRING, RDF_DATATYPE_BYTE));
        Ontology ontology = om.loadOntologyFromOntologyDocument(sds);
        log.info(requestUUID + " |   Loaded target ontology " + om.getOntology(iri).getOntologyID() + " to ONT manager");
        return ontology;
    }

    private Git prepareGitRepository(TransportConfigCallback transportConfigCallback, String requestUUID) {
        String temporaryGitFolderPath = GIT_REPO_LOCAL_PATH + "-" + requestUUID;
        try {
            File sourceGitFolder = new File(GIT_REPO_LOCAL_PATH);
            if (sourceGitFolder.isDirectory()) {
                log.info(requestUUID + " | Setting up git credentials");

                // ======================== go back to develop and pull to latest commit =================================


                File temporaryGitFolder = new File(temporaryGitFolderPath);
                log.info(requestUUID + " | Copying repository folder " + GIT_REPO_LOCAL_PATH + " to " + temporaryGitFolderPath);
                FileUtils.copyDirectory(sourceGitFolder, temporaryGitFolder);
                log.info(requestUUID + " | Opening temporary git folder " + temporaryGitFolderPath);
                Git git = Git.open(temporaryGitFolder);
                log.info(requestUUID + " |   Having repository: " + git.getRepository().getDirectory());

                log.info(requestUUID + " | Executing git checkout " + GIT_MAIN_BRANCH + " on " + temporaryGitFolderPath);
                git.checkout().setName(GIT_MAIN_BRANCH).call();
                log.info(requestUUID + " | Executing git reset --hard on " + temporaryGitFolderPath);
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
                log.info(requestUUID + " | Executing git pull on " + temporaryGitFolderPath);
                git.pull().setTransportConfigCallback(transportConfigCallback).call();

                log.info(requestUUID + " |   Current branch: " + git.getRepository().getFullBranch());
                return git;
            } else {
                log.error("Git repository is not cloned - folder not found. The repo should be pre-cloned in dockerfile. Check the docker log and make sure the repository folder is accessible.");
            }
        } catch (Exception e) {
            log.error(e.toString());
            e.printStackTrace();
            return null;
        }
        return null;
    }

    private void saveOntologyToFile(OWLOntology owlOntology, String path, boolean addedCoreImport) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        owlOntology.saveOntology(out);
        String result = out.toString("UTF-8").replace(RDF_DATATYPE_BYTE, RDF_DATATYPE_STRING);
        result = result.replaceAll("-->\n\n\n {4}", "-->\n\n    ");
        if (addedCoreImport) {
            result = result.replace("\n        " + owlImportCore, "");
        }
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);
        writer.write(result);
        writer.close();
    }

//    private String getOntologyImports() throws Exception {
//        return Util.fetchTextResponse(owlManagerConfig.getOntologyApiEndpoint() + "/bob/ontology-imports?endpoint=" + NAMESPACE);
//    }

    private void recursiveTrackRelatedOntologies(String ontologyText, String ontologyFileDirectoryPath, Set<String> relatedOntologyIris, Set<String> loadedOntologyIrisInOwlManager) {
        String ontologyIRI = getOntologyIRI(ontologyText);
        if (loadedOntologyIrisInOwlManager.contains(ontologyIRI) || relatedOntologyIris.contains(ontologyIRI)) {
            return;
        }
//        if (//oom.contains(IRI.create("http://ontology.vse.cz/core")) ||
//            ontologyText.contains("<owl:Ontology rdf:about=\"http://ontology.vse.cz/core\">") ||
//            ontologyText.contains("<owl:Ontology rdf:about=\"http://ontology.vse.cz/dataquality\">") ||
//            ontologyText.contains("<owl:Ontology rdf:about=\"http://ontology.vse.cz/event\">") ||
//            ontologyText.contains("<owl:Ontology rdf:about=\"http://ontology.vse.cz/policy\">") ||
//            ontologyText.contains("<owl:Ontology rdf:about=\"http://ontology.vse.cz/currency\">")
//        ) {
//            return;
//        }
        relatedOntologyIris.add(ontologyIRI);
        List<String> importIris = Util.getImportIriMatches(ontologyText);
        importIris.forEach(importIri -> {
            String importFileName = iriFileMap.get(importIri);
            if (importFileName == null) {
                return;
            }
            try {
                String importOntologyFileText = Util.getFileContent(ontologyFileDirectoryPath + importFileName);
                recursiveTrackRelatedOntologies(importOntologyFileText, ontologyFileDirectoryPath, relatedOntologyIris, loadedOntologyIrisInOwlManager);
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
    }

    private Set<String> getRelatedOntologyIris(String ontologyText, String ontologyFileDirectoryPath, Set<String> loadedOntologyIrisInOwlManager) {
        Set<String> relatedOntologyIris = new HashSet<>();
        recursiveTrackRelatedOntologies(ontologyText, ontologyFileDirectoryPath, relatedOntologyIris, loadedOntologyIrisInOwlManager);
        return relatedOntologyIris;
    }

    private Map<String, String> getIriFileMap () {
        List<String> gitFolderListing = new ArrayList<>();
        Map<String, String> res = new HashMap<>();
        try {
            Stream<Path> walk = Files.walk(Paths.get(GIT_REPO_LOCAL_PATH + File.separator + "files" + File.separator));
            List<String> paths = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
            gitFolderListing.addAll(paths);

            gitFolderListing.forEach((file) -> {
                boolean ontologyIriFound = false;
                try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                    String line;
                    while (!ontologyIriFound && ((line = reader.readLine()) != null) && reader.getLineNumber() <= 200) {
                        if (line.contains("<owl") && line.contains(":Ontology ") && line.contains(":about=") && line.contains(">")) {
                            ontologyIriFound = true;
                            String iri = getOntologyIRIFromLine(line);
                            File f = new File(file);
                            res.put(iri, f.getName());
                        }
                    }
                    if (!ontologyIriFound) {
                        log.warn("Did not find ontology iri for file: " + file);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private String getOntologyIRIFromLine(String line) {
        return line.replaceAll("<owl(ns)?:Ontology rdf(ns)?:about=\"", "")
                .replace("\">", "")
                .replace("\"/>", "")
                .replaceAll(" ", "");
    }

    private String getOntologyIRI(String ontologyText) {
        String[] lines = ontologyText.split("[\r\n]+");
        for(String line : lines) {
            if (line.contains("<owl") && line.contains(":Ontology ") && line.contains(":about=") && line.contains(">")) {
                return getOntologyIRIFromLine(line);
            }
        }
        return "";
    }
}

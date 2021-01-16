package cz.vse.miraql.controller;

import cz.vse.miraql.config.MiraqlConfig;
import cz.vse.miraql.model.OntologyDependencyDependantCount;
import cz.vse.miraql.model.SparqlResponse;
import cz.vse.miraql.utils.SshTransportConfigCallback;
import cz.vse.miraql.utils.Util;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.jena.update.UpdateAction;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.Ontology;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Slf4j
@CrossOrigin
public class MiraqlControllerYugioh {

    private final List<String> GIT_REVIEWERS = Arrays.asList("patkom", "bjoand1", "danbjo", "joalun", "erinor", "petgru");
    private final MiraqlConfig owlManagerConfig;
    private final String NAMESPACE = "master";
    private final String API_PREFIX = "/" + NAMESPACE;
    private final String ONTOLOGY_DOMAIN = "http://ontology.vse.cz/";
    private final String GIT_SERVICE_PREFIX = "https://github.com/stash/projects/PROJECT/repos";
    private final String GIT_REPO_NAME = "yugioh-ontology";
    private final String GIT_REPO_LOCAL_PATH = File.separator + GIT_REPO_NAME;
    private final String RDF_DATATYPE_STRING = "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\"";
    private final String RDF_DATATYPE_BYTE = "rdf:datatype=\"http://www.w3.org/2001/XMLSchema#byte\"";
    private final String GIT_MAIN_BRANCH = "develop";

    private final Map<String, Map<String, String>> externalLibraries = new HashMap<String, Map<String, String>>(){{
        put("http://purl.org/dc/terms", new HashMap<String, String>(){{
            put("url", GIT_SERVICE_PREFIX + "/" + GIT_REPO_NAME + "/raw/files/dcterms.rdf");
            put("file", "dcterms.rdf");
            put("content", "");
        }});
        put("http://purl.org/dc/elements/1.1", new HashMap<String, String>(){{
            put("url", GIT_SERVICE_PREFIX + "/" + GIT_REPO_NAME + "/raw/files/dcelements.rdf");
            put("file", "dcelements.rdf");
            put("content", "");
        }});
        put("http://www.w3.org/2004/02/skos/core", new HashMap<String, String>(){{
            put("url", GIT_SERVICE_PREFIX + "/" + GIT_REPO_NAME + "/raw/files/skos.rdf");
            put("file", "skos.rdf");
            put("content", "");
        }});
        put(ONTOLOGY_DOMAIN + "core", new HashMap<String, String>(){{
            put("url", GIT_SERVICE_PREFIX + "/" + GIT_REPO_NAME + "/raw/files/core.owl");
            put("file", "");
            put("content", "");
        }});
    }};
    private final OWLOntologyLoaderConfiguration ontologyLoaderConfiguration =
            new OWLOntologyLoaderConfiguration()
                    .addIgnoredImport(IRI.create("http://ontology.vse.cz/eam"))
                    .addIgnoredImport(IRI.create("http://ontology.vse.cz/core"))
                    .addIgnoredImport(IRI.create("http://ontology.vse.cz/policy"))
                    .addIgnoredImport(IRI.create("http://ontology.vse.cz/country/iso3166-1"))
                    .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);

    private final OWLOntologyWriterConfiguration ontologyWriterConfiguration =
            new OWLOntologyWriterConfiguration();
    //                    .withRemapAllAnonymousIndividualsIds(false)
//                    .withSaveIdsForAllAnonymousIndividuals(false);
    @Autowired
    public MiraqlControllerYugioh(@NonNull MiraqlConfig owlManagerConfig) {
        this.owlManagerConfig = owlManagerConfig;
        externalLibraries.keySet().forEach(iri -> {
            log.info("Downloading external ontology library " + iri);
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
            if (externalLibrary.get("content").isEmpty() && !externalLibrary.get("file").isEmpty()) {
                try {
                    String fallbackOntologyText = Util.getFileContent(GIT_REPO_LOCAL_PATH + File.separator + "files" + File.separator + externalLibrary.get("file"));
                    if (!fallbackOntologyText.contains("<owl:Ontology rdf:about=\"" + iri + "\">")) {
                        fallbackOntologyText = fallbackOntologyText.replace("</rdf:RDF>", "<owl:Ontology rdf:about=\"" + iri + "\"></owl:Ontology></rdf:RDF>");
                    }
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
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "OK");
        return new ResponseEntity<>(new Gson().toJson(resp), HttpStatus.OK);
    }

    @GetMapping(path = API_PREFIX + "/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> test() {
        OntologyManager om = OntManagers.createONT();
        externalLibraries.keySet().forEach(iri -> {
            try {
                String ontologyText = externalLibraries.get(iri).get("content").replaceAll("<owl:imports.*>", "");
                Ontology externalOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(ontologyText));
                log.info("Loaded ontology " + externalOntology.getOntologyID().getOntologyIRI());
            } catch (Exception e) {
                log.error(e.toString());
            }
        });
        List<String> gitFolderListing = new ArrayList<>();
        try {
            Stream<Path> walk = Files.walk(Paths.get(File.separator + GIT_REPO_NAME + File.separator + "files" + File.separator));
            List<String> paths = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
            gitFolderListing.addAll(paths);
        } catch (Exception e) {
            gitFolderListing.add(e.toString());
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("test", "ok");
        resp.put("gitFolder", gitFolderListing);
        String response = new Gson().toJson(resp);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = API_PREFIX + "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> update(final OAuth2Authentication oAuth2Authentication,
                                         @RequestParam(value = "sparql") final String sparql,
                                         @RequestParam(value = "targetOntologyIri") final String targetOntologyIri,
                                         @RequestParam(value = "userEmailAddress") final String userEmailAddress,
                                         @RequestParam(value = "changeDescription") final String changeDescription) {
        try {
            TransportConfigCallback transportConfigCallback = new SshTransportConfigCallback();
            String requestUUID = Util.getShortUUID();
            log.info(requestUUID + " | Preparing repository");
            String temporaryGitFolderPath = GIT_REPO_LOCAL_PATH + "-" + requestUUID;
            Git git = prepareGitRepository(transportConfigCallback, requestUUID);
            if (git == null) {
                throw new Exception("Could not establish ontology git folder");
            }
            log.info(requestUUID + " | Checking git credentials ");
            git.push().setTransportConfigCallback(transportConfigCallback).call();


            String ontologyFileDirectoryPath = git.getRepository().getDirectory().toString()
                    .replaceAll("\\\\\\.git$", "")
                    .replaceAll("/\\.git$", "") + File.separator + "files" + File.separator;
            log.info(requestUUID + " | Having ontology file directory path: " + ontologyFileDirectoryPath);

            log.info(requestUUID + " | Downloading ontology imports map");
            // =============== get the imports hierarchy and create a map of ontology files and their iri ============
            String ontologyImportsResponse = getOntologyImports();
            SparqlResponse sparqlResponse = new ObjectMapper().readValue(ontologyImportsResponse, SparqlResponse.class);
//            Set<String> uniqueOntologyIris = new HashSet<>();
//            Map<String, List<String>> ontologyDependencyDependantMap = new HashMap<>();

            // =============== count the number of dependants each that each ontology has ============================
            Map<String, Number> ontologyDependencyDependantCountMap = new HashMap<>();
            sparqlResponse.getResults().getBindings().forEach(sparqlBinding -> {
                String dependant = sparqlBinding.getOntology().get("value");
                String dependency = sparqlBinding.getImports().get("value");
//                uniqueOntologyIris.add(dependant);
//                uniqueOntologyIris.add(dependency);

                if (ontologyDependencyDependantCountMap.containsKey(dependency)) {
//                    ontologyDependencyDependantMap.get(dependency).add(dependant);
                    ontologyDependencyDependantCountMap.put(dependency, ontologyDependencyDependantCountMap.get(dependency).intValue() + 1);
                } else {
                    ontologyDependencyDependantCountMap.put(dependency, 1);
//                    ontologyDependencyDependantMap.put(dependency, new ArrayList<String>() {{ add(dependant); }});
                }
                if (!ontologyDependencyDependantCountMap.containsKey(dependant)) {
                    ontologyDependencyDependantCountMap.put(dependant, 0);
                }
            });
//            log.info(Util.JSONStringify(ontologyDependencyDependantMap));
//            log.info(Util.JSONStringify(ontologyDependantDependencyMap));
//            log.info(Util.JSONStringify(uniqueOntologyIris.toArray()));
//            log.info(Util.JSONStringify(ontologyDependencyDependantCountMap));

            log.info(requestUUID + " | Ontology imports map setup successful");

            log.info(requestUUID + " | Setting up ontologyDependencyDependantCounts");
            // =============== get a sorted list of ontology iris to add to the ontology manager======================
            List<OntologyDependencyDependantCount> ontologyDependencyDependantCounts = new ArrayList<>();
            ontologyDependencyDependantCountMap.keySet().forEach(iri -> {
                Number count = ontologyDependencyDependantCountMap.get(iri);
                OntologyDependencyDependantCount oodc = new OntologyDependencyDependantCount(iri, count);
                ontologyDependencyDependantCounts.add(oodc);
            });
            Collections.sort(ontologyDependencyDependantCounts);
//            log.info(Util.JSONStringify(ontologyDependencyDependantCounts));


            // ================== load the owl files in the correct order (dependency first, dependent after) ========
            OntologyManager om = OntManagers.createONT();
            om.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);
            log.info(requestUUID + " | Loading external ontology libs to ont manager");
            externalLibraries.keySet().forEach(iri -> {
                try {
                    String fetchedOntologyText = externalLibraries.get(iri).get("content").replaceAll("<owl:imports.*>", "");
                    Ontology externalOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(fetchedOntologyText));
                    log.info(requestUUID + " | Loaded ontology to ont manager " + externalOntology.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });
            // =============== keep a reference list of each ontology model and its filename =========================
            Map<String, Ontology> ontologys = new HashMap<>();
            log.info(requestUUID + " | Loading imported ontologies to ont manager");
            ontologyDependencyDependantCounts.forEach(oodc -> {
                String fileName = Util.getFileNameFromIri(ONTOLOGY_DOMAIN, oodc.getIri());
                File file = new File(ontologyFileDirectoryPath + fileName);
                if (!file.isFile()) {
                    return;
                }
                try {
//                    String ontologyText = Util.getFileContent(ontologyFileDirectoryPath + fileName);
                    // ==== load empty ontologies that is listed in the owl:imports statement and is not in any file ==
//                    Util.getImportIriMatches(ontologyText).forEach(importIri -> {
//                        if (!importIri.startsWith("http://ontology.vse.cz/eam")) {
//                            loadBlankOntology(om, importIri);
//                        }
//                    });
//                    log.info(requestUUID + " | Loaded file " + fileName + " to ONTAPI OntologyManager");

                    // ================= create a map of ontology models and their file names ========================
                    String ontologyText = Util.getFileContent(ontologyFileDirectoryPath + fileName).replaceAll(RDF_DATATYPE_STRING, RDF_DATATYPE_BYTE);
                    Ontology importedOntology = om.loadOntologyFromOntologyDocument(new StringDocumentSource(ontologyText));
                    ontologys.put(fileName, importedOntology);
                    log.info(requestUUID + " | Loaded imported ontology to ont manager " + importedOntology.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });

            // ================== do the sparql update in the specified ontology (use the map) =======================
            String targetFileName = Util.getFileNameFromIri(ONTOLOGY_DOMAIN, targetOntologyIri);
//            log.info(Util.getFileContent(ontologyFileDirectoryPath + targetFileName));
            Ontology ontology = ontologys.get(targetFileName);
            UpdateAction.parseExecute(sparql, ontology.asGraphModel());

            // ================================= save the ontology to its file =======================================
            String ontologyText = Util.getOntologySourceString(ontology);
//            log.info(ontologyText);
            OWLOntologyManager oom = OntManagers.createOWL();
            oom.setOntologyLoaderConfiguration(ontologyLoaderConfiguration);
            oom.setOntologyWriterConfiguration(ontologyWriterConfiguration);

            log.info(requestUUID + " | Loading external ontology libs to owl manager");
            // ========================= add external libraries/ontologies to ontology manager =======================
            externalLibraries.keySet().forEach(iri -> {
                try {
                    String fetchedOntologyText = externalLibraries.get(iri).get("content").replaceAll("<owl:imports.*>", "");
                    OWLOntology oo = oom.loadOntologyFromOntologyDocument(new StringDocumentSource(fetchedOntologyText));
                    log.info(requestUUID + " | Loaded ontology to owl manager" + oo.getOntologyID().getOntologyIRI());
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });

            // ======= use the iris in owl:imports to import these ontologies ========================================
            log.info(requestUUID + " | Loading imported ontologies to owl manager");
            Util.getImportIriMatches(ontologyText).forEach(importIri -> {
                try {
                    String importFileName = Util.getFileNameFromIri(ONTOLOGY_DOMAIN, importIri);
                    String importOntologyFileText = Util.getFileContent(ontologyFileDirectoryPath + importFileName);
                    oom.loadOntologyFromOntologyDocument(new StringDocumentSource(importOntologyFileText));
                    log.info(requestUUID + " | Loaded imported ontology to owl manager " + importFileName + " " + importIri);
                } catch (Exception e) {
                    log.error(e.toString());
                }
            });
            OWLOntology owlOntology = oom.loadOntologyFromOntologyDocument(new StringDocumentSource(ontologyText));
            log.info(requestUUID + " | Loaded modified file to owl manager " + targetFileName);
//            oom.ontologies().forEach(ontology -> {
//                log.info(ontology.getOntologyID().getOntologyIRI().toString());
//            });
            log.info(requestUUID + " | Saving modified file " + targetFileName);
            saveOntologyToFile(owlOntology, ontologyFileDirectoryPath + targetFileName);

            // ================================= create branch, add changes, commit and push =========================
            String newBranchName = "MIRAQL_EDIT_" + requestUUID;
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
            log.info(requestUUID + " | Deleting temporary git folder " + temporaryGitFolderPath);
            FileUtils.deleteDirectory(new File(temporaryGitFolderPath));

            log.info(requestUUID + " | Creating Pull Request");
            String pullRequestCreationResponse = Util.createPullRequest(GIT_SERVICE_PREFIX, GIT_REPO_NAME, newBranchName, changeDescription, GIT_REVIEWERS);
            log.info(requestUUID + " | Done :)");
            return new ResponseEntity<>(pullRequestCreationResponse, HttpStatus.OK);
        } catch (Exception e) {
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
                log.info(requestUUID + " | Having repository: " + git.getRepository().getDirectory());

                log.info(requestUUID + " | Executing git checkout " + GIT_MAIN_BRANCH + " on " + GIT_REPO_LOCAL_PATH);
                git.checkout().setName(GIT_MAIN_BRANCH).call();
                log.info(requestUUID + " | Executing git reset --hard on " + GIT_REPO_LOCAL_PATH);
                git.reset().setMode(ResetCommand.ResetType.HARD).call();
                log.info(requestUUID + " | Executing git pull on " + GIT_REPO_LOCAL_PATH);
                git.pull().setTransportConfigCallback(transportConfigCallback).call();

                log.info(requestUUID + " | Current branch: " + git.getRepository().getFullBranch());
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

    private void saveOntologyToFile(OWLOntology owlOntology, String path) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        owlOntology.saveOntology(out);
        String result = out.toString("UTF-8").replace(RDF_DATATYPE_BYTE, RDF_DATATYPE_STRING);
        result = result.replaceAll("-->\n\n\n {4}", "-->\n\n    ");
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path, false), StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);
        writer.write(result);
        writer.close();
    }

    private String getOntologyImports() throws Exception {
        return Util.fetchTextResponse(owlManagerConfig.getOntologyApiEndpoint() + "/bob/ontology-imports?endpoint=" + NAMESPACE);
    }
}


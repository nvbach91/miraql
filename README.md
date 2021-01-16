![img](https://i.chzbgr.com/thumb800/3899653/h75CA01C6/a-funny-list-of-owl-memes)

# MIRAQL, a REST API service for agile Git-based management of OWL Ontologies via SPARQL
This component provides an API to manipulate a network of ontology files using SPARQL update queries

## REST API service example
```js
fetch('http://localhost:8080/miraql/namespace/update', { 
    method: 'POST', 
    headers: { 
        'accept': 'application/json', 
        'content-type': 'application/json' 
    },
    body: JSON.stringify({
        sparql: '...',
        targetOntologyIri: '...',
        userEmailAddress: '...',
        changeDescription: '...'
    }),
});
```

#### SPARQL UPDATE on a specific concept in ontology-eam and create a Pull Request    
POST `http://localhost:8080/kb/update`  
Params  
`sparql=DELETE { ... } INSERT { ... } WHERE { ... }`  
`targetOntologyIri=http://ontology.domain.com/ygo/`  
`userEmaillAddress=email`  
`changeDescription=commit message`


### Concept

![image](https://user-images.githubusercontent.com/20724910/104810979-a1069080-57f8-11eb-8d8e-cf08fdbda4fa.png)


### Front-end JS code examples

#### 1.0 Querying for concept data from Blazegraph
```js
const selectedConcept = 'http://ontology.vse.cz/dataset#annualReportsSe';
// variable ?p  = predicate
// variable ?pl = predicate label
// variable ?o  = object
const query = `
  SELECT ?p ?pl ?o {
    <${selectedConcept}> ?p ?o .
    OPTIONAL { ?p rdfs:label ?pl . }
  }
`;
fetch('http://localhost:8082/namespace/kb/sparql',
  {
    'headers': {
      'accept': 'application/sparql-results+json',
      'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
    },
    'body': `query=${encodeURIComponent(query)}`,
    'method': 'POST',
  }
).then((res) => res.json()).then((res) => {
  // transform query results and render edit form
});
```

#### 1.1 Concept data query response from Blazegraph
```js
{
    "head": {
        "vars": [
            "p",
            "pl",
            "o"
        ]
    },
    "results": {
        "bindings": [ // <-- this is the results array, each result is a json object with "p", "pl" and "o" properties, it is recommended
                      //     that you transform these results into a state object that can be updated more easily
            {
                "p": { // <-- this is the predicate IRI which should be used as the property key for the editable form state object
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasDataControllerInCountry"
                },
                "o": { // <-- this is the value of the predicate, it can be of type "uri" or "literal"
                    "type": "uri",
                    "value": "http://ontology.vse.cz/country/iso3166-1#SE"
                },
                "pl": { // <-- if the result contains a label for the predicate, it is displayed as the form input name
                    "xml:lang": "en",
                    "type": "literal",
                    "value": "has data controller in country"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasDataManager"
                },
                "o": { // <-- if the type is "literal" and the datatype is "xsd:string", render a textarea input
                    "datatype": "http://www.w3.org/2001/XMLSchema#string",
                    "type": "literal",
                    "value": "Susanna Nyman"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasDataOwner"
                },
                "o": {
                    "datatype": "http://www.w3.org/2001/XMLSchema#string",
                    "type": "literal",
                    "value": "Tobias Forss"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": { // <-- if the type is "uri" render a multi select input, data for other options in this select can be retrieved
                       //     using code example #2.0
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/legalground#LegitimateInterest"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Credit"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Marketing"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Profiling"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#VerificationAndControl"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/typeofdata#ContactData"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/typeofdata#FinancialSpecifications"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/typeofdata#IdentificationData"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/typeofdata#ProfessionEducationTraining"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Analyze"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Directory"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#Statistics"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/purpose#CommunityInformation"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/availability#2"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/confidentiality#2"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/integrity#3"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/hasPolicy"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/policy/typeofdata#ImageRecording"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://purl.org/dc/elements/1.1/created"
                },
                "o": { // <-- if the type is "literal" and the datatype is "xsd:dateTime", render a date picker, or a plain text input
                    "datatype": "http://www.w3.org/2001/XMLSchema#dateTime",
                    "type": "literal",
                    "value": "2018-12-05T14:08:07.000Z"
                }
            },
            {
                "p": {  // <-- some values are not meant to be editable, this one is an example, render a disabled select input, you can
                        //     detect it using an enum of non-editable predicates
                    "type": "uri",
                    "value": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                },
                "o": {
                    "type": "uri",
                    "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/DataSet"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://www.w3.org/2000/01/rdf-schema#comment"
                },
                "o": { // <-- if the type is "literal" and has a "xml:lang", render a textarea input and a small language tag required input
                       //     that accepts two letters as per language codes
                    "xml:lang": "en",
                    "type": "literal",
                    "value": "comment"
                }
            },
            {
                "p": { // <-- if there are multiple predicates with the same IRI, it means that those predicates has multiple values, in this
                       //     case: 2 comments in 2 languages
                    "type": "uri",
                    "value": "http://www.w3.org/2000/01/rdf-schema#comment"
                },
                "o": {
                    "xml:lang": "sv",
                    "type": "literal",
                    "value": "Bokslut AB, HB/HK, Ek Fören, Kommuner och Landsting"
                }
            },
            {
                "p": {
                    "type": "uri",
                    "value": "http://www.w3.org/2000/01/rdf-schema#isDefinedBy" // <-- this is another non-editable predicate, but if the dataset is to be created, only one isDefinedBy is allowed per dataset
                },
                "o": {
                    "type": "uri",
                    "value": "http://ontology.vse.cz/eam/dataset/se" // <-- this is the targetOntologyIri
                }
            },
            {
                "p": { // <-- the edit form should allow users to create new values for an existing predicate, also it should allow users to
                       //     delete existing values of any predicate, as well as creating a new predicate
                    "type": "uri",
                    "value": "http://www.w3.org/2000/01/rdf-schema#label"
                },
                "o": {
                    "xml:lang": "en",
                    "type": "literal",
                    "value": "Årsredovisningar"
                }
            }
        ]
    }
}
```


#### 2.0 Querying for select options of a predicate from Blazegraph
```js
// each object property predicate can have a set of possible iri values
// you can fetch them from our triple store using the following SPARQL query
const predicateIri = 'http://ontology.vse.cz/eam/hasPolicy';
const query = `
  SELECT DISTINCT ?value {
    [] <${predicateIri}> ?value .
  }
`;
fetch('http://localhost:8082/namespace/kb/sparql',
  {
    'headers': {
      'accept': 'application/sparql-results+json',
      'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
    },
    'body': `query=${encodeURIComponent(query)}`,
    'method': 'POST',
  }
).then((res) => res.json()).then((res) => {
  // transform query results and render form
});
```


#### 2.1 Select options query response from Blazegraph
```js
{
  "head" : {
    "vars" : [ "value" ]
  },
  "results" : {
    "bindings" : [ {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#Consent"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#LegitimateInterest"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Credit"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Marketing"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Profiling"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#VerificationAndControl"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ContactData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#DebtCollectionInformation"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#FinancialSpecifications"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#IdentificationData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#Other"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ProfessionEducationTraining"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#Contract"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Analyze"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Directory"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Other"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#Statistics"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#Children"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ConsumerInterests"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ElectronicIdentificationData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#FamilyComposition"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#HouseFeatures"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#LifeStyleData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#Memberships"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#PersonalCharacteristics"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#PsychologicalData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#PublicInterest"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#LegalObligation"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#CommunityInformation"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ElectronicalLocalizationData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#SoundRecordings"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legalground#LocalLegislation"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#RealEstateInformation"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/availability#1"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/availability#2"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/availability#3"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/availability#4"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/confidentiality#1"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/confidentiality#2"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/confidentiality#3"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/confidentiality#4"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/integrity#1"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/integrity#2"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/integrity#3"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/integrity#4"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legislation/se#CreditInformationAct"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/legislation/se#YGL"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/purpose#NA"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#BiometricalIdentificationData"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#ImageRecording"
      }
    }, {
      "value" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/policy/typeofdata#VehicleData"
      }
    } ]
  }
}
```


#### 3.0 Form state object example
```js
// You can parse the conceptData to an object to make rendering and updates more efficient, this is how I do it in React
const conceptData = {
  "http://ontology.vse.cz/eam/hasDataControllerInCountry": {
    "propertyLabel": "has data controller in country",
    "valueType": "uri",
    "values": [
      {
        "value": "http://ontology.vse.cz/country/iso3166-1#SE"
      }
    ]
  },
  "http://ontology.vse.cz/eam/hasDataManager": {
    "propertyLabel": "http://ontology.vse.cz/eam/hasDataManager",
    "valueType": "literal",
    "values": [
      {
        "value": "Susanna Nyman",
        "datatype": "http://www.w3.org/2001/XMLSchema#string"
      }
    ]
  },
  "http://ontology.vse.cz/eam/hasDataOwner": {
    "propertyLabel": "http://ontology.vse.cz/eam/hasDataOwner",
    "valueType": "literal",
    "values": [
      {
        "value": "Tobias Forss",
        "datatype": "http://www.w3.org/2001/XMLSchema#string"
      }
    ]
  },
  "http://ontology.vse.cz/eam/hasPolicy": {
    "propertyLabel": "http://ontology.vse.cz/eam/hasPolicy",
    "valueType": "uri",
    "values": [
      {
        "value": "http://ontology.vse.cz/policy/legalground#LegitimateInterest"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Analyze"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Credit"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Directory"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Marketing"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Statistics"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#VerificationAndControl"
      },
      {
        "value": "http://ontology.vse.cz/policy/typeofdata#ContactData"
      },
      {
        "value": "http://ontology.vse.cz/policy/typeofdata#IdentificationData"
      },
      {
        "value": "http://ontology.vse.cz/policy/typeofdata#FinancialSpecifications"
      },
      {
        "value": "http://ontology.vse.cz/policy/typeofdata#ProfessionEducationTraining"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#Profiling"
      },
      {
        "value": "http://ontology.vse.cz/policy/purpose#CommunityInformation"
      },
      {
        "value": "http://ontology.vse.cz/policy/availability#2"
      },
      {
        "value": "http://ontology.vse.cz/policy/confidentiality#2"
      },
      {
        "value": "http://ontology.vse.cz/policy/integrity#3"
      },
      {
        "value": "http://ontology.vse.cz/policy/typeofdata#ImageRecording"
      }
    ]
  },
  "http://purl.org/dc/elements/1.1/created": {
    "propertyLabel": "http://purl.org/dc/elements/1.1/created",
    "valueType": "literal",
    "values": [
      {
        "value": "2018-12-05T14:08:07.000Z",
        "datatype": "http://www.w3.org/2001/XMLSchema#dateTime"
      }
    ]
  },
  "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": {
    "propertyLabel": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
    "valueType": "uri",
    "values": [
      {
        "value": "http://www.w3.org/2002/07/owl#NamedIndividual"
      },
      {
        "value": "http://ontology.vse.cz/eam/DataSet"
      }
    ]
  },
  "http://www.w3.org/2000/01/rdf-schema#comment": {
    "propertyLabel": "http://www.w3.org/2000/01/rdf-schema#comment",
    "valueType": "literal",
    "values": [
      {
        "value": "comment",
        "lang": "en"
      },
      {
        "value": "Bokslut AB, HB/HK, Ek Fören, Kommuner och Landsting",
        "lang": "sv"
      }
    ]
  },
  "http://www.w3.org/2000/01/rdf-schema#isDefinedBy": {
    "propertyLabel": "http://www.w3.org/2000/01/rdf-schema#isDefinedBy",
    "valueType": "uri",
    "values": [
      {
        "value": "http://ontology.vse.cz/eam/dataset/se"
      }
    ]
  },
  "http://www.w3.org/2000/01/rdf-schema#label": {
    "propertyLabel": "http://www.w3.org/2000/01/rdf-schema#label",
    "valueType": "literal",
    "values": [
      {
        "value": "Årsredovisningar",
        "lang": "en"
      }
    ]
  }
}
```


#### 4.0 Creating SPARQL UPDATE
```js
const getUpdateDatatype = (dt) => {
  if (dt === 'http://www.w3.org/2001/XMLSchema#string') {
    return 'http://www.w3.org/2001/XMLSchema#byte';// this is used due to owlapi not outputing string data explicitly
  }
  return dt;
};
 
const conceptData = { /* see above */ };
const selectedConcept = '';
 
 
// more on SPARQL UPDATE here: http://www.iro.umontreal.ca/~lapalme/ift6281/sparql-1_1-cheat-sheet.pdf
const query = `
  DELETE {
    ${Object.keys(conceptData).map((property, index) => {
      return `  ?target <${property}> ?o${index} .`;
    }).join('\n')}
  }
  INSERT {
    ${Object.keys(conceptData).map((property) => {
      const prop = conceptData[property];
      const valueType = prop.valueType;
      return prop.values.map(({ value, datatype, lang }) => {
        return `  ?target <${property}> ${valueType === 'uri' ? '<' : '"'}${value.replace(/\n/g,'\\n')}${valueType === 'uri' ? '>' : '"'}${lang ? `@${lang}` : ''}${datatype ? `^^<${getUpdateDatatype(datatype)}>` : ''} .`
      }).join('\n');
    }).join('\n')}
  }
  WHERE {
    BIND(<${selectedConcept}> AS ?target)
    ${Object.keys(conceptData).map((property, index) => {
      return `  ?target <${property}> ?o${index} .`;
    }).join('\n')}
  }
`;
 
 
 
// # example of a SPARQL query that creates a new Dataset
// DELETE {
//
// }
// INSERT {
//   ?dataset a <owl:NamedIndividual> .
//   ?dataset a <eam:DataSet> .
//   ?dataset rdfs:isDefinedBy ?ontology .
//   ?dataset rdfs:label "dataset name"@en .
//   ?dataset rdfs:label "dataset name"@de .
//   ?dataset eam:hasPolicy ?policy .
// }
// WHERE {
//   BIND(<...> AS ?dataset) .  # this is the new dataset IRI
//   BIND(<...> AS ?ontology) . # this is the iri of the ontology file
//   BIND(<...> AS ?policy) .   # this is the iri of a policy
// }
```


#### 5.0 Sending the SPARQL UPDATE and get a Pull Request link
```js
const serializeBody = (obj) => {
  const str = [];
  for (let p in obj) {
    if (obj.hasOwnProperty(p)) {
      str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
    }
  }
  return str.join("&");
};
 
 
// get the target ontology iri from the predicate rdfs:isDefinedBy
let targetOntologyIri = conceptData['http://www.w3.org/2000/01/rdf-schema#isDefinedBy'];
if (!targetOntologyIri) {
  return console.error('This concept does not have isDefinedBy property');
}
targetOntologyIri = targetOntologyIri.values[0].value;
const userEmailAddress = 'userEmailAddress';
// this is the commit message in the Pull Request
const changeDescription = 'changed something very important';
const query = '...';
fetch('http://localhost:8080/miraql/kb/update',
  {
    'headers': {
      'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
    },
    'body': serializeBody({
      sparql: query,
      targetOntologyIri,
      userEmailAddress,
      changeDescription,
    }),
    'method': 'POST',
  }
).then((res) => res.json()).then((res) => {
  // pullRequestLink
  alert(res.message);
});
```


### More insights

#### 2.2 Get information on possible properties of a class
```sparql
# GET all possible properties (and the datatypes of their values) for a class
SELECT DISTINCT ?p ?pc ?dv ?cv WHERE {
  BIND(<http://ontology.vse.cz/eam/DataSet> AS ?targetClass)
  ?d a ?targetClass .
  OPTIONAL {
    ?d ?p ?v .
    BIND(DATATYPE(?v) AS ?dv) .
  }
  OPTIONAL {   
    ?v a ?cv .
  }
  OPTIONAL { ?p a ?pc . }
}
 
 
# p = property
# pc = property class (super property)
# dv = datatype of value (if applicable)
# cv = class of value (if applicable)
```
- EXAMPLE RESPONSE in CSV (text/csv):
```csv 
p,pc,dv,cv
http://ontology.vse.cz/eam/hasDataManager,http://www.w3.org/2002/07/owl#DatatypeProperty,http://www.w3.org/2001/XMLSchema#string,
http://ontology.vse.cz/eam/hasDataOwner,http://www.w3.org/2002/07/owl#DatatypeProperty,http://www.w3.org/2001/XMLSchema#string,
http://www.w3.org/2000/01/rdf-schema#label,,http://www.w3.org/1999/02/22-rdf-syntax-ns#langString,
http://ontology.vse.cz/eam/hasDataControllerInCountry,http://www.w3.org/2002/07/owl#ObjectProperty,,
http://ontology.vse.cz/eam/hasPolicy,http://www.w3.org/2002/07/owl#ObjectProperty,,
http://www.w3.org/1999/02/22-rdf-syntax-ns#type,,,
http://www.w3.org/1999/02/22-rdf-syntax-ns#type,,,http://www.w3.org/2002/07/owl#Class
http://www.w3.org/2000/01/rdf-schema#isDefinedBy,,,http://www.w3.org/2002/07/owl#Ontology
http://www.w3.org/2000/01/rdf-schema#comment,,http://www.w3.org/1999/02/22-rdf-syntax-ns#langString,
http://purl.org/dc/elements/1.1/creator,http://www.w3.org/2002/07/owl#AnnotationProperty,http://www.w3.org/2001/XMLSchema#string,
http://purl.org/dc/elements/1.1/created,http://www.w3.org/2002/07/owl#AnnotationProperty,http://www.w3.org/2001/XMLSchema#dateTime,
http://ontology.vse.cz/eam/isProcessor,http://www.w3.org/2002/07/owl#DatatypeProperty,http://www.w3.org/2001/XMLSchema#boolean,
http://www.w3.org/2002/07/owl#deprecated,,http://www.w3.org/2001/XMLSchema#boolean,
```

#### 2.3 SPARQL results for insight 2.2
```js
{
  "head" : {
    "vars" : [ "p", "pc", "dv", "cv" ]
  },
  "results" : {
    "bindings" : [ {
      "p" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/eam/hasDataManager"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#DatatypeProperty" // <- this indicates that the value will be a literal (string/date/number etc.)
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#string" // <- this specifies the datatype of that literal
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/eam/hasDataOwner"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#DatatypeProperty"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#string"
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2000/01/rdf-schema#label"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString" // <- this means that the literal value can have language tags
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/eam/hasDataControllerInCountry"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#ObjectProperty" // <- this indicates that the value will be an object (a IRI pointer to that object)
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/eam/hasPolicy"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#ObjectProperty"
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" // <- missing information on this one? not a problem, consider this a stapple and can be hardcoded
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
      },
      "cv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#Class" // <- owl#Class is the class of the value which is the dataset class
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2000/01/rdf-schema#isDefinedBy"
      },
      "cv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#Ontology" // <- this tells you that this concept (dataset) can be defined in an instance of owl#Ontology
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://purl.org/dc/elements/1.1/created"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#AnnotationProperty" // <- this indicates that it is gonna be a literal (usually)
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#dateTime" // <- this specifies the type of that literal
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2000/01/rdf-schema#comment"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://ontology.vse.cz/eam/isProcessor"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#DatatypeProperty"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#boolean" // <- wow, a boolean datatype
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#deprecated"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#boolean" // <- another boolean datatype, actually, you can create a map of all datatypes, but not all of them are relevant to us (https://www.w3.org/2011/rdf-wg/wiki/XSD_Datatypes - SPARQL column)
      }
    }, {
      "p" : {
        "type" : "uri",
        "value" : "http://purl.org/dc/elements/1.1/creator"
      },
      "pc" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2002/07/owl#AnnotationProperty"
      },
      "dv" : {
        "type" : "uri",
        "value" : "http://www.w3.org/2001/XMLSchema#string"
      }
    } ]
  }
}
```

#### 2.4 Get a list of possible isDefinedBy values for datasets
```sparql
SELECT DISTINCT ?ontology WHERE {
  ?ontology a owl:Ontology .
  FILTER(REGEX(STR(?ontology),'/dataset/'))
}
```

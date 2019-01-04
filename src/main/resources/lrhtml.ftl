<!doctype html>
<html class="no-js" lang="">

<head>
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <title>Phenotype/Genotype Likelihood Ratio Analysis</title>
  <meta name="description" content="">
  <meta name="viewport" content="w idth=device-width, initial-scale=1, shrink-to-fit=no">

  <style>
@charset "UTF-8";

article, aside, footer, header, main, nav, section {
  display: block;
}

html, body, h1, ul, li, a, p, article, aside, footer, header, main, nav, section {
  padding: 0;
  margin: 0;
}

h2,h3 { padding 5px; }

.banner {
  background-color: #11233b;
  color: white;
  padding: 10px 20px;
}

body {
  width: 1120px;
  margin-left: auto;
  margin-right: auto;
  background-color: #f0f0f0;
  font-family: Helvetica, Arial, sans-serif;
  font-size: 15px;
}

nav {
  background-color: #20416c;
  padding: 5px;
  margin-top: 1px;
}

div#navi li a {
  color: white;
}

div#navi li {
  display: inline;
  margin-left: 15px;
  margin-right: 15px;
  font-size: 20px;
  font-variant: small-caps;
  font-weight: bold;
}

li {
  margin-left: 15px;
  margin-right: 15px;
  font-size: 12px;
}

section {
  background-color: #bbbbbb;
  margin-top: 10px;
  padding: 5px;
}

article {
  background-color: white;
  margin-top: 5px;
  margin-bottom 5px;
  padding: 10px 15px;
}

aside {
  background-color: #bbbbbb;
  width: 1px;
  float: right;
  padding: 20px;
  margin-top: 10px;
}

footer {
  clear: both;
  background-color: #20416c;
  color: white;
  padding: 5px 20px;
}

table.redTable {
  border: 2px solid #A40808;
  background-color: #EEE7DB;
  width: 100%;
  text-align: center;
  border-collapse: collapse;
}
table.redTable td, table.redTable th {
  border: 1px solid #AAAAAA;
  padding: 3px 2px;
}
table.redTable tbody td {
  font-size: 13px;
}
table.redTable tr:nth-child(even) {
  background: #F5C8BF;
}
table.redTable thead {
  background: #A40808;
}
table.redTable thead th {
  font-size: 19px;
  font-weight: bold;
  color: #FFFFFF;
  text-align: center;
  border-left: 2px solid #A40808;
}
table.redTable thead th:first-child {
  border-left: none;
}

table.redTable tfoot {
  font-size: 13px;
  font-weight: bold;
  color: #FFFFFF;
  background: #A40808;
}
table.redTable tfoot td {
  font-size: 13px;
}
table.redTable tfoot .links {
  text-align: right;
}
table.redTable tfoot .links a{
  display: inline-block;
  background: #FFFFFF;
  color: #A40808;
  padding: 2px 8px;
  border-radius: 5px;
}
 table.minimalistBlack {
    border: 3px solid #000000;
    width: 100%;
    text-align: left;
    border-collapse: collapse;
 }
 table.minimalistBlack td, table.minimalistBlack th {
   border: 1px solid #000000;
   padding: 5px 4px;
 }

 table.minimalistBlack.red td {
  background: red;
  }

  table.minimalistBlack tbody td {
    font-size: 13px;
  }
  table.minimalistBlack thead {
    background: #CFCFCF;
    background: -moz-linear-gradient(top, #dbdbdb 0%, #d3d3d3 66%, #CFCFCF 100%);
    background: -webkit-linear-gradient(top, #dbdbdb 0%, #d3d3d3 66%, #CFCFCF 100%);
    background: linear-gradient(to bottom, #dbdbdb 0%, #d3d3d3 66%, #CFCFCF 100%);
    border-bottom: 3px solid #000000;
  }
  table.minimalistBlack thead th {
    font-size: 15px;
    font-weight: bold;
    color: #000000;
    text-align: left;
  }
  table.minimalistBlack tfoot {
    font-size: 14px;
    font-weight: bold;
    color: #000000;
    border-top: 3px solid #000000;
  }
  table.minimalistBlack tfoot td {
    font-size: 14px;
  }
/* We use this to color cells with 'pathogenic' variants */
   td.red {
    background: rgba(137,145,180,0.3);
    }
}
  </style>
</head>

<body>
  <!--[if lte IE 9]>
    <p class="browserupgrade">You are using an <strong>outdated</strong> browser. Please <a href="https://browsehappy.com/">upgrade your browser</a> to improve your experience and security.</p>
  <![endif]-->
<header class="banner">
    <h1>LR2PG: Likelihood Ratio Analysis of Phenotype and Genotype Data</h1>
</header>

<nav>
    <div id="navi">
        <ul>
            <li><a href="#sample">Sample</a></li>
            <li><a href="#diff">Differential diagnosis</a></li>
            <li><a href="#othergenes">Remaining genes</a></li>
            <li><a href="#about">About</a></li>
        </ul>
    </div>
</nav>
<main>
  <section>
    <a name="sample"/>
    <h2>Sample name: ${sample_name}</h2>
    <article>
      <header>
        <h3>Observed phenotypic features:</h3>
      </header>
      <p>
        <ul>
          <#list  observedHPOs as hpo>
          <li>${hpo}</li>
          </#list>
        </ul>
      </p>
    </article>
  </section>

  <section>
   <article>
    <a name="diff"/>
    <h2>Differential diagnosis: posterior probability above ${postprobthreshold}</h2>

    <p>Top differential diagnoses:
    <ol>
    <#list diff as dd>
        <li><a href="#${dd.anchor}">${dd.diseaseName}</a></li>
     </#list>
    </ol>
    </p>
    </article>
    </section>


    <#list diff as dd>
    <section>
        <article>
         <a name="${dd.anchor}"/>
          <header>
            <h3>(${dd.rank}) ${dd.diseaseName} [<a href="${dd.url}" target="_blank">${dd.diseaseCurie}</a>]</h3>
          </header>
           <a name="${dd.anchor}"/>
          <p>
           <table class="redTable">
             <tr><th>Pretest probability</th><th>Posttest probability</th></tr>
             <tr><td>${dd.pretestprob}</td><td>${dd.posttestprob}</td></tr>
           </table>
          </p>
          <br/>
          <#if dd.hasVariants=="yes">

          <table class="minimalistBlack">
          <thead>
          <tr>
          <th>Positiion</th>
          <th>Pathogenicity score</th>
          <th>Max. pop. frequency</th>
          <th>Genotype</th>
          <th>ClinVar</th>
          <th>Annotation</th>
          </tr>
          </thead>
              <#list  dd.varlist as svar>
              <tr>
                <td>${svar.chromosome}:${svar.position}${svar.ref}&gt;${svar.alt}</td>
                <#if svar.isInPathogenicBin() >
                <td class="red">${svar.pathogenicity}</td>
                <#else>
                 <td>${svar.pathogenicityScore}</td>
                </#if>
                <td>${svar.frequency}%</td>
                <td>${svar.gtype}</td>
                <td>${svar.clinvar}</td>
                <td>
                <ul>
                  <#list svar.annotationList as annot>
                   <li> ${annot.hgvsCdna} ${annot.hgvsProtein} ${annot.variantEffect}</li>
                  </#list>
                  </ul>
                </td>
                 </tr>
               </#list>
               <#if dd.hasExplanation() >
               <tr><td>Genotype score LR:</td><td colspan="5">${dd.explanation}</td></tr>
               </#if>
               </table>
          <#else>
          <p><table class="minimalistBlack">
                       <thead>
                       <tr>
                       <th>${dd.noVariantsFound}</th>
                       </tr></table></p>
          </#if>
          <br/>
          <div style="border:1px solid black; text-align:center;">
          ${dd.svg}
          </div>
        </article>
         </section>
      </#list>



  <section>
    <a name="othergenes"/>
     <h3>Genes/Diseases with low posttest probability:</h3>
    <article>
      <!--<header>
        <h3>Genes/Diseases with low posttest probability:</h3>
      </header> -->

      <p>Variants were identified in the following genes. The posttest probability of diseases
      assosiated with these genes was below ${postprobthreshold}. The table shows the total count of
      variants found in the genes.</p>

      <table class="redTable">
        <tr><th style="width:60%">Disease</th><th>Gene</th><th>Post test probability</th><th>variant count</th></tr>
        <#list improbdiff as ipd>
          <tr><td><a href="https://omim.org/${ipd.diseaseId}" target="_blank">${ipd.diseaseName}</a></td><td>${ipd.geneName}</td><td>${ipd.posttestProbability}</td><td>${ipd.varcount}</td></tr>
        </#list>
       </table>
    </article>
  </section>
</main>

  <section>
   <article>
    <a name="about"/>
    <h2>Explanation</h2>

    <p>LR2PG is a tool for exploring exome or genome sequencing data obtained for an individual with suspected rare genetic disease.
    LR2PG uses phenotypic features that describe the clinical manifestations observed in the individual and expressed
    as <a href="http://www.human-phenotype-ontology.org">Human Phenotype Ontology</a> (HPO) terms as well as the
    sequence variants found in the exome or genome file to derive a list of candidate diagnoses with estimated posterior
    probabilities. LR2PG is intended as a resource to aide diagnosticians and does not make a diagnosis itself. The
    results of LR2PG should not be construed as medical advice and should always be reviewed by medical professionals.</p>
    <p>Full documentation that explains how to use LR2PG can be found TODO.</p>
    </article>
    </section>
<footer>
  <p>LR2PG &#160; 2019</p>
</footer>
</body>
</html>
<!doctype html>
<html class="no-js" lang="">

<head>
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <title>Phenotype/Genotype Likelihood Ratio Analysis</title>
  <meta name="description" content="">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

  <style>
@charset "UTF-8";

article, aside, footer, header, main, nav, section {
  display: block;
}

html, body, h1, h2, h3, ul, li, a, p, article, aside, footer, header, main, nav, section {
  padding: 0;
  margin: 0;
}

.banner {
  background-color: #11233b;
  color: white;
  padding: 10px 20px;
}

body {
  width: 960px;
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
  padding: 10px 15px;
}

main {
  width: 960px;
  float: left;
  margin-bottom: 10px;
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
    <a name="diff"/>
    <h2>Differential diagnosis: posterior probability above ${postprobthreshold}</h2>
    <#list diff as dd>
        <article>
          <header>
            <h3>${dd.diseaseName}</h3>
          </header>
          <p>${dd.diseaseCurie}
            <ul>
              <li>Rank: ${dd.rank}</li>
              <li>Pretest probability: ${dd.pretestprob}</li>
              <li>Posttest probability: ${dd.posttestprob}</li>
            </ul>
          </p>
          <#if dd.hasVariants=="yes">
            <ul>
              <#list  dd.varlist as svar>
                <li>chr${svar.chromAsInt}:${svar.position}${svar.ref}&gt;${svar.alt}<br/>
                  Pathogenicity score: ${svar.pathogenicity}<br/>
                  Maximum population frequency: ${svar.frequency}<br/>
                  Genotype: ${svar.gtype}<br/>
                  ClinVar: ${svar.clinvar}<br/>
                  <#list svar.annotationList as annot>
                    ${annot.hgvsCdna} ${annot.hgvsProtein} ${annot.variantEffect}
                  </#list>
                </li>
               </#list>
            </ul>
          </#if>
          ${dd.svg}
        </article>
      </#list>
  </section>


  <section>
    <a name="othergenes"/>
     <h3>Genes/Diseases with low posttest probability:</h3>
    <article>
      <!--<header>
        <h3>Genes/Diseases with low posttest probability:</h3>
      </header> -->
      <table>
        <tr><td>Disease</td><td>Gene</td><td>Post test probability</td><td>variant count</td></tr>
        <#list improbdiff as ipd>
        <#if ipd.diseaseName??>IPD DN not null!<#else>IPD DNnull!</#if>
          <tr><td>${ipd.diseaseName!"-"}</td><td>${ipd.geneName!"-"}</td><td>${ipd.posttestProbability!"-"}</td><td>${ipd.varcount!"-"}</td></tr>
        </#list>
       </table>
    </article>
  </section>
</main>

<!--    <aside>
DO NOT USE ASIDE ELEMENT
      <h2>LR2PG</h2>
      <p>Likelihood ratio 2 Phenotype/Genotyupe analysis</p>
    </aside> -->

<footer>
  <p>LR2PG 2018</p>
</footer>
</body>
</html>
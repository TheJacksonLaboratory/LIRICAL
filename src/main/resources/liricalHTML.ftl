<!doctype html>
<html class="no-js" lang="">

<head>
  <meta charset="utf-8">
  <meta http-equiv="x-ua-compatible" content="ie=edge">
  <title>LIRICAL</title>
  <meta name="description" content="">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

  <style>
@import url("https://www.jax.org/_res/css/modules/jax-base/p01-fonts.css");
@import url("https://www.jax.org/_res/css/modules/fonts-extended.css");

* {
    -moz-box-sizing: border-box;
    -webkit-box-sizing: border-box;
    box-sizing: border-box
}

html, body, h1, li, a, article, aside, footer, header, main, nav, section {
	padding: 0;
	margin: 0;
}

html, body {
	font-size:14px;
}

body {
	font-family:"DIN Next", Helvetica, Arial, sans-serif;
	line-height:1.25;
	background-color:#e0e3ea;
}


body > header, nav, main, body > section, footer {
max-width:1200px;
margin-left:auto;
margin-right:auto;
}

@media(min-width:1440px) {
body > header, nav, main, body > section, footer {
    width:83.3333%;
    max-width:unset;
    }
}

main, body > section {
	margin-top:1.5rem;
	margin-bottom:1.5rem;
}

body > header, body > section {
	padding:2.1rem 2rem 1.6rem;
}

.fr {
  float: right;
}

a[href] {
	color:#05396b;
}

a[href]:hover {
	color:#009ed0;
}

p {
	padding:0;
	margin:0.75rem 0;
}

h1 {
	font-family:"DIN Next", Helvetica, Arial, sans-serif;
	font-weight:700;
	font-size:1.8rem;
	line-height:1;
}

.center {
  text-align: center;
}
/* Your really should address semantic issues with your markup that make selectors like this necessary */

main > section > a[name="othergenes"] > h3,
h2 {
	font-family:"DIN Next", Helvetica, Arial, sans-serif;
	font-weight:700;
	font-size:1.5rem;
	line-height:1;
	margin:0 0 0.5rem;
	padding:0;
}

h3 {
	font-family:"DIN Next", Helvetica, Arial, sans-serif;
	font-weight:700;
	font-size:1.2rem;
	line-height:1;
	margin:0 0 0.5rem;
	padding:0;
}



main ul, main ol {
	margin:0.5rem 0 0.5rem 1.4rem;
	padding:0;
}

main li {
	margin:0.25rem 0;
	padding:0;
}

.banner {
	background-color: #05396b;
	color: white;
}

nav {
	background-color: #05396b;
	margin-top:1px;
	overflow:auto;
	zoom:1;
	padding:0;
}

nav a[href] {
	color:white;
	text-decoration:none;
	color:rgba(255,255,255,0.8);
	font-size:1.2rem;
	display:block;
	padding:1rem;
	font-weight:400;
}

nav li:last-child a[href] {
	padding-right:2.25rem;
}

nav a[href]:hover {
	color:#05396b;
	background-color:#04c3ff;
}

#navi ul {
	display:table;
	float:right;
	margin:0;
}

#navi li {
	display:block;
	float:left;
}

main > section:first-child {
	margin-top:1.5rem;
	margin-bottom:1.5rem;
	background-color:white;
	padding:2.1rem 2rem 1.6rem;

}

main > section {
	margin-top:1.5rem;
	margin-bottom:0;
	background-color:white;
	padding: .5rem;

}

main > section > article {
	padding: 1.5rem;
	margin-top:1px;
	background-color:white;
}

table {
	border-collapse: collapse;
	width:100%;
	margin:0.5rem 0;
}

th, td {
	text-align:left;
	padding:0.4rem 0.5rem 0.25rem;
}

th {
	background-color: #e0e3ea;
	border-bottom:1px solid white;
}

table.redTable {
	width:auto;
	min-width:50%;
}

table.redTable td {
	background-color:#f0f3fa;
}

table.posttest {
	width:auto;
	min-width:50%;
	margin-left:auto;
    margin-right:auto;
}

table.posttest td {
    line-height: 40px;
}

table.posttest th  {font-size:1.5rem;}

table.posttest tr:nth-child(even) {background: #F5F5F5}
table.posttest tr:nth-child(odd) {background: #FFF}

table.minimalistBlack th,
table.minimalistBlack td {
	border:2px solid #e0e3ea;
}

table.minimalistBlack.red td {
	background: red;
}

td.red {
	background-color:#f0f3fa;
}


a[name="othergenes"] table.redTable {

}

a[name="othergenes"] table.redTable td.disease {
	font-size:0.928rem;
	padding-top:0.35rem;
	padding-bottom:0.15rem;
	text-transform: lowercase
}

a[name="othergenes"] table.redTable > tbody > tr:nth-child(even) > td {
	background-color:white;
}

a[name="othergenes"] table.redTable > tbody > tr:hover > td {
	background-color:#cceaff;
}

a[name="othergenes"] table.redTable a {
	text-decoration: none;
	display:block;
}

a[name="othergenes"] table.redTable a:hover {
	text-decoration: underline;
}

a[name="othergenes"] table.redTable a::first-letter {
	text-transform: uppercase;
}

/* Create two equal columns that floats next to each other */
.column {
  float: left;
  width: 50%;
  padding: 10px;
}

/* Clear floats after the columns */
.row:after {
  content: "";
  display: table;
  clear: both;
}

footer {
	background-color: #05396b;
	color: white;
	padding: 1rem 2rem;
}

/* The following links are in the SVG for the differentials */
a.svg:link, a.svg:visited {
  cursor: pointer;
}

a.svg text,
text a.svg {
  fill: blue; /* Even for text, SVG uses fill over color */
  text-decoration: underline;
}

a.svg:hover, a.svg:active {
  outline: dotted 1px blue;
}

.features-title {
  background-color: #05396b;
  color: white;
}

.features-title:nth-child(1) {
  border-right: 2px solid white;
}

.features-data {
  background-color: #e0e3ea;
}

.features-data:nth-child(1) {
  border-right: 2px solid white;
}
.no-list-style {
  list-style-type: none;
}

#tooltip {
  background: #05396b;
  border: 1px solid black;
  border-radius: 0;
  padding: 5px;
  color: white;
}

.table-btn {
    display: block;
    font-weight: bold;
    padding: 10px;
    background-color: #05396b;
    width: fit-content;
    color: white;
    cursor: pointer;
}

#hide-other-genes-table, #other-genes-table {
  display: none;
}

</style>
</head>

<body>
  <!--[if lte IE 9]>
    <p class="browserupgrade">You are using an <strong>outdated</strong> browser. Please <a href="https://browsehappy.com/">upgrade your browser</a> to improve your experience and security.</p>
  <![endif]-->
<header class="banner">
    <h1><font color="#FFDA1A">LIRICAL</font>: <font color="#FFDA1A">LI</font>kelihood <font color="#FFDA1A">R</font>atio
    <font color="#FFDA1A">I</font>nterpretation of <font color="#FFDA1A">C</font>linical
    <font color="#FFDA1A">A</font>bnorma<font color="#FFDA1A">L</font>ities</h1>
</header>

  <nav>
      <div id="navi">
          <ul>
              <li><a href="#sample">Sample</a></li>
              <li><a href="#diff">Differential diagnosis</a></li>
              <li><a href="#othergenes">Remaining genes</a></li>
              <li><a href="#explain">Definitions</a></li>
              <li><a href="#about">About</a></li>
          </ul>
      </div>
  </nav>
  <main>
    <section>
      <a name="sample"></a>
      <h2 class="center">Sample name: ${sample_name!"n/a"}</h2>
      <article>
        <div class="row">
          <div class="column features-title center">
            <h3>Observed Phenotypic Features</h2>
          </div>
          <div class="column features-title center">
              <h3>Excluded Phenotypic Features</h3>
          </div>
        </div>
        <div class="row" style="background-color:#e0e3ea;">
            <div class="column features-data">
            <p>
              <ul class="no-list-style">
              <#list  observedHPOs as hpo>
                  <li>${hpo}</li>
              </#list>
              </ul>
            </p>
            <#if errorlist?has_content>
              <p>The following errors were encountered while processing the Phenopacket.</p>
              <ul>
              <#list errorlist as error>
                  <li>${error}</li>
              </#list>
              </ul>
            </#if>
          </div>
          <div class="column features-data">
            <p>
                <#if excludedHPOs?has_content>
                  <ul class="no-list-style">
                  <#list excludedHPOs as hpo>
                    <li>Excluded: ${hpo}</li>
                  </#list>
                  </ul>
                <#else>
                  None provided
                </#if>
            </p>
          </div>
        </div>

        <#if vcf_file?has_content>
          <p>VCF file: ${vcf_file}</p>
        </#if>
        <#if phenopacket_file?has_content>
            <p>Phenopacket file: ${phenopacket_file}</p>
        </#if>
        <#if yaml?has_content>
            <p>YAML configuration file: ${yaml}</p>
        </#if>
        <p>LIRICAL analysis performed on ${analysis_date}.</p>
      </article>
    </section>

    <section>
      <article>
        <a name="diff"></a>
        <h2>Top differential diagnoses</h2>
        <p>${topdifferentialcount}</p>
        <div style="border:1px solid black; text-align:center;">
            <table class="posttest">
                <tr><th>Rank</th><th>Post-test probability</th><th>Disease</th><th>ID</th><th>Profile match</th><th>Composite LR (log)</th><th>Gene</th></tr>
                    <#list sparkline as sprk>
                    <tr><td>${sprk.rank}</td><td>${sprk.posttestBarSvg}</td><td><a href="#diagnosis${sprk.rank}">${sprk.diseaseName}</a></td><td>${sprk.diseaseAnchor}</td><td>${sprk.sparklineSvg}</td><td>${sprk.compositeLikelihoodRatio}</td><td>${sprk.geneSymbol}</td></tr>
                </#list>
            </table>
        </div>
      </article>
      <#list diff as dd>
          <article>
          <a name="${dd.anchor}"></a>
            <header>
              <h3>(${dd.rank}) ${dd.diseaseName} [<a href="${dd.url}" target="_blank">${dd.diseaseCurie}</a>]</h3>
            </header>
            <p>
            <table class="redTable">
              <tr><th>Pretest probability</th><th>Log<sub>10</sub> composite likelihood ratio</th><th>Posttest probability</th></tr>
              <tr><td>${dd.pretestprob}</td><td>${dd.compositeLR}</td><td>${dd.posttestprob}</td></tr>
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
                  <td>${svar.ucsc}</td>
                  <#if svar.isInPathogenicBin() >
                  <td class="red">${svar.pathogenicityScore!"n/a"}</td>
                  <#else>
                  <td>${svar.pathogenicityScore!"n/a"}</td>
                  </#if>
                  <td>${svar.frequency}%</td>
                  <td>${svar.gtype}</td>
                  <td>${svar.clinvar}</td>
                  <td>
                  <ul class="no-list-style">
                    <#list svar.annotationList as annot>
                    <li>${annot.accession}: ${annot.hgvsCdna} ${annot.hgvsProtein} ${annot.variantEffect}</li>
                    </#list>
                    </ul>
                  </td>
                  </tr>
                </#list>
                <#if dd.hasGenotypeExplanation() >
                <tr><td>Genotype score LR:</td><td colspan="5">${dd.genotypeExplanation}</td></tr>
                </#if>
                </table>
            <#else>
            </#if>
            <br/>
            <div style="border:1px solid black; text-align:center;">
            ${dd.svg}
            </div>
          </article>
        </#list>
    </section>
    <section>
      <a name="othergenes"></a>
      <article>
        <h2>Genes/Diseases with low posttest probability:</h2>
        <p>Variants were identified in the following genes. The posttest probability of diseases
        associated with these genes was below ${postprobthreshold}. The table shows the total count of
        variants found in the genes.</p>
        <a id="show-other-genes-table" class="table-btn" onclick="showTable()">Show Table</a>
        <a id="hide-other-genes-table" class="table-btn" onclick="hideTable()">Hide Table</a>
        <table class="redTable" id="other-genes-table">
          <tr><th style="width:60%">Disease</th><th>Gene</th><th>Post test probability</th><th>variant count</th></tr>
          <#list improbdiff as ipd>
            <tr><td class="disease"><a href="https://omim.org/${ipd.diseaseId}" target="_blank">${ipd.diseaseName}</a></td><td><i>${ipd.geneName}</i></td><td>${ipd.posttestProbability}</td><td>${ipd.varcount}</td></tr>
          </#list>
        </table>
      </article>
    </section>

    <section>
      <a name="explain"></a>
      <article>
        <h2>Definitions</h2>
        <p>LIRICAL calculates likelihood ratios for each HPO feature and for the genotype (if applicable).
        It displays detailed information for the top differential diagnoses (by default all diseases
        with a posttest probability above ${postprobthreshold} and at least 5; these thresholds can be
        adjusted if desired). The following text provides brief explanations of the symbols used by
        LIRICAL to explain how phenotype likelihood ratio scores were generaed.</p>
        <ul class="no-list-style">
          <li><b>E</b>: Exact match between query term and disease term.</li>
          <li><b>Q&lt;D</b>: Query term is a child of disease term.</li>
          <li><b>D&lt;Q</b>: Disease term is child of query term.</li>
          <li><b>Q~D</b>: Query term and disease term are related but are separated by more than one edge.</li>
          <li><b>NM</b>: No common ancestor match besides the root of the ontology.</li>
          <li><b>X</b>: Query term is explicitly annotated as being not present in disease</li>
          <li><b>XX</b>: Term excluded by query and explicitly annotated as being not present in disease</li>
          <li><b>XA</b>: Term excluded by query and not explicitly annotated as being present in disease</li>
          <li><b>XP</b>: Term excluded by query but is explictly annotated as being present in disease</li>
          <li><b>U</b>: Flag for unusual background query (please report to developers)</li>
        </ul>
      </article>
      </section>
      <section>
        <a name="about"></a>
        <article>
          <h2>About</h2>
            <p>LIRICAL is a tool for exploring exome or genome sequencing data obtained for an individual with suspected rare genetic disease.
      LIRICAL uses phenotypic features that describe the clinical manifestations observed in the individual and expressed
      as <a href="http://www.human-phenotype-ontology.org">Human Phenotype Ontology</a> (HPO) terms as well as the
      sequence variants found in the exome or genome file to derive a list of candidate diagnoses with estimated posterior
      probabilities. LIRICAL is intended as a resource to aide diagnosticians and does not make a diagnosis itself. The
      results of LIRICAL should not be construed as medical advice and should always be reviewed by medical professionals.</p>
      <p>See LIRICAL's <a href="https://lirical.readthedocs.io/en/latest/" target="_blank">online documentation</a>.</p>

          <h4><i>This LIRICAL run had the following configuration.<i></h4>
          <ul>
            <#if hpoVersion?has_content>
              <li>Human Phenotype Ontology version: ${hpoVersion}</li>
            </#if>
            <#if transcriptDatabase?has_content>
              <li>Transcript database: ${transcriptDatabase}</li>
            </#if>
            <#if n_good_quality_variants?has_content>
              <li>High-quality variants: ${n_good_quality_variants}</li>
            </#if>
            <#if n_filtered_variants?has_content>
              <li>Variants removed due to quality filter: ${n_filtered_variants}</li>
            </#if>
            <#if genesWithVar?has_content>
                <li>Genes found to have at least one variant: ${genesWithVar}</li>
            </#if>
              <#if exomiserPath?has_content>
              <li>Path to Exomiser database: ${exomiserPath}</li>
            </#if>
              <#if global_mode?has_content>
              <li>Global analysis mode: ${global_mode}</li>
              </#if>
          </ul>
          </p>
        </article>
      </section>
      <span id="tooltip" display="none" style="position: absolute; display: none;"></span>
  </main>
  <footer>
    <p>LIRICAL &copy; 2019</p>
  </footer>

  <script>
  function showTooltip(evt, text) {
    let tooltip = document.getElementById("tooltip");
    tooltip.innerText = text;
    tooltip.style.display = "block";
    tooltip.style.left = evt.pageX + 10 + 'px';
    tooltip.style.top = evt.pageY + 10 + 'px';
  }

  function hideTooltip() {
    var tooltip = document.getElementById("tooltip");
    tooltip.style.display = "none";
  }

  function showTable() {
    var table = document.getElementById("other-genes-table");
    table.style.display = "block";
    var showtablebtn = document.getElementById("show-other-genes-table");
    showtablebtn.style.display = "none";

    var hidetablebtn = document.getElementById("hide-other-genes-table");
    hidetablebtn.style.display = "block";
  }

   function hideTable() {
    var table = document.getElementById("other-genes-table");
    table.style.display = "none";
    var showtablebtn = document.getElementById("show-other-genes-table");
    showtablebtn.style.display = "block";

    var hidetablebtn = document.getElementById("hide-other-genes-table");
    hidetablebtn.style.display = "none";
  }
  </script>
</body>
</html>

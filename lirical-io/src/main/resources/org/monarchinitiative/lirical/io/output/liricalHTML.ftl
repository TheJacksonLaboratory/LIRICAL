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
            font-size: 14px;
        }

        body {
            font-family: "DIN Next", Helvetica, Arial, sans-serif;
            line-height: 1.25;
            background-color: #e0e3ea;
        }


        body > header, nav, main, body > section, footer {
            max-width: 1200px;
            margin-left: auto;
            margin-right: auto;
        }

        @media (min-width: 1440px) {
            body > header, nav, main, body > section, footer {
                width: 83.3333%;
                max-width: unset;
            }
        }

        main, body > section {
            margin-top: 1.5rem;
            margin-bottom: 1.5rem;
        }

        body > header, body > section {
            padding: 2.1rem 2rem 1.6rem;
        }

        .fr {
            float: right;
        }

        a[href] {
            color: #05396b;
        }

        a[href]:hover {
            color: #009ed0;
        }

        p {
            padding: 0;
            margin: 0.75rem 0;
        }

        h1 {
            font-family: "DIN Next", Helvetica, Arial, sans-serif;
            font-weight: 700;
            font-size: 1.8rem;
            line-height: 1;
        }

        .center {
            text-align: center;
        }

        .navi .report-id {
            color: white;
            display: inline-flex;
            padding: 1rem;
            margin: 0;
            font-weight: 400;
        }

        /* Your really should address semantic issues with your markup that make selectors like this necessary */

        main > section > a[name="othergenes"] > h3,
        h2 {
            font-family: "DIN Next", Helvetica, Arial, sans-serif;
            font-weight: 700;
            font-size: 1.5rem;
            line-height: 1;
            margin: 0 0 0.5rem;
            padding: 0;
        }

        h3 {
            font-family: "DIN Next", Helvetica, Arial, sans-serif;
            font-weight: 700;
            font-size: 1.2rem;
            line-height: 1;
            margin: 0 0 0.5rem;
            padding: 0;
        }


        main ul, main ol {
            margin: 0.5rem 0 0.5rem 1.4rem;
            padding: 0;
        }

        main li {
            margin: 0.25rem 0;
            padding: 0;
        }

        .banner {
            background-color: #05396b;
            color: white;
        }

        .banner .banner-highlight {
            color: #FFDA1A;
        }

        .banner .tagline {
            font-size: 1.4rem;
        }

        .banner .tagline .banner-highlight {
            font-size: 1.6rem
        }

        nav {
            background-color: #05396b;
            margin-top: 1px;
            overflow: auto;
            zoom: 1;
            padding: 0;
        }

        nav a[href] {
            color: white;
            text-decoration: none;
            color: rgba(255, 255, 255, 0.8);
            font-size: 1.2rem;
            display: block;
            padding: 1rem;
            font-weight: 400;
        }

        nav li:last-child a[href] {
            padding-right: 2.25rem;
        }

        nav a[href]:hover {
            color: #05396b;
            background-color: #04c3ff;
        }

        .navi ul {
            display: table;
            float: right;
            margin: 0;
        }

        .navi li {
            display: block;
            float: left;
        }

        main > section:first-child {
            margin-top: 1.5rem;
            margin-bottom: 1.5rem;
            background-color: white;
            padding: 2.1rem 2rem 1.6rem;

        }

        main > section {
            margin-top: 1.5rem;
            margin-bottom: 0;
            background-color: white;
            padding: .5rem;

        }

        main > section > article {
            padding: 1.5rem;
            margin-top: 1px;
            background-color: white;
        }

        table {
            border-collapse: collapse;
            width: 100%;
        }

        th, td {
            text-align: left;
            padding: 0.4rem 0.5rem 0.25rem;
        }

        th {
            background-color: #e0e3ea;
            border-bottom: 1px solid white;
        }

        table.redTable {
            width: auto;
            min-width: 50%;
            margin: 0 auto;
        }

        table.redTable td {
            background-color: #f0f3fa;
        }

        table.posttest {
            width: auto;
            min-width: 50%;
            margin-left: auto;
            margin-right: auto;
            border: 1px solid black;
        }

        table.posttest td {
            line-height: 40px;
        }

        table.posttest th {
            font-size: 1.5rem;
        }

        table.posttest tr:nth-child(even) {
            background: #F5F5F5
        }

        table.posttest tr:nth-child(odd) {
            background: #FFF
        }

        td.posttest {
            font-size: 1.3rem;
        }

        table.minimalistBlack th,
        table.minimalistBlack td {
            border: 2px solid #e0e3ea;
        }

        table.minimalistBlack.red td {
            background: red;
        }

        td.red {
            background-color: #f0f3fa;
        }


        a[name="othergenes"] table.redTable {

        }

        a[name="othergenes"] table.redTable td.disease {
            font-size: 0.928rem;
            padding-top: 0.35rem;
            padding-bottom: 0.15rem;
            text-transform: lowercase
        }

        a[name="othergenes"] table.redTable > tbody > tr:nth-child(even) > td {
            background-color: white;
        }

        a[name="othergenes"] table.redTable > tbody > tr:hover > td {
            background-color: #cceaff;
        }

        a[name="othergenes"] table.redTable a {
            text-decoration: none;
            display: block;
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

        .diff-dg .dg-header {
            text-transform: uppercase;
        }

        .diff-dg  .dg-header .dg-title .dg-id  {
            font-size:1.4rem;
            color:darkgray;
        }

        .diff-dg .dg-header .dg-rank {
            color:darkgray;
        }

        .hgvs-symbol .entrez-id {
            font-size:1rem;
            color:darkgray;
            text-transform: unset;
        }

        .diff-dg .dg-id a, .hgvs-symbol .entrez-id a {
            text-decoration: none;
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

        .toggle-threshold-btn, .extra-annotations-toggle {
            color: blue;
            cursor: pointer;
        }

        #hide-other-genes-table, #other-genes-table {
            display: none;
        }

        #hide-symbol-table, #symbol-table {
            display: none;
        }

        .lr-contributions {
            border: 1px solid black;
            text-align: center;
        }

        .dd-variant-row-failing, .extra-annotations {
            display: none;
        }

        .elevate-container {
            box-shadow: 0 2px 4px -1px rgb(0 0 0 / 20%), 0 4px 5px 0 rgb(0 0 0 / 14%), 0 1px 10px 0 rgb(0 0 0 / 12%);
        }

        .genotype-explanation {
            font-style: italic;
        }

        .hgvs-symbol {
            font-style: italic;
        }

        .dd-seperator {
            width: 75%;
            border-color: gray;
            border-width: 0.25px;
        }
    </style>
</head>

<body>
<!--[if lte IE 9]>
<p class="browserupgrade">You are using an <strong>outdated</strong> browser. Please <a href="https://browsehappy.com/">upgrade
    your browser</a> to improve your experience and security.</p>
<![endif]-->
<header class="banner">
    <h1>
            <span class="banner-highlight">LIRICAL</span>&nbsp;
            <span class="tagline">
                <span class="banner-highlight">LI</span>kelihood&nbsp;
                <span class="banner-highlight">R</span>atio
                <span class="banner-highlight">I</span>nterpretation of <span class="banner-highlight">C</span>linical
                <span class="banner-highlight">A</span>bnorma<span class="banner-highlight">L</span>ities
            </span>
    </h1>
</header>
<!-- -->
<nav>
    <div class="navi">
        <h3 class="report-id">${resultsMeta.sampleName!"n/a"} - ${resultsMeta.analysisDate}</h3>
        <ul>
            <li><a href="#phenotypes">Sample</a></li>
            <li><a href="#diff">Differential diagnosis</a></li>
            <li><a href="#othergenes">Remaining genes</a></li>
            <li><a href="#explain">Definitions</a></li>
            <li><a href="#about">About</a></li>
        </ul>
    </div>
</nav>
<main>
    <section class="sample-summary elevate-container">
        <a id="Phenotypes"></a>
        <h1 class="center">Phenotypic features of ${resultsMeta.sampleName!"n/a"}</h1>
        <article>
            <div class="row">
                <div class="column features-title center">
                    <h3>Observed Phenotypic Features</h3>
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
                            <li>${hpo}</li>
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
        </article>
    </section>

    <section class="diff-dg-sparklines elevate-container">
        <article>
            <a id="diff"></a>
            <div class="center">
                <h1>Top Differential Diagnoses</h1>
                <p>${topdifferentialcount}</p>
            </div>
            <div style="text-align:center;">
                <#if phenotypeOnly>
                    <table class="posttest">
                        <tr>
                            <th>Rank</th>
                            <th>Post-test probability</th>
                            <th>Disease</th>
                            <th>ID</th>
                            <th>Phenotypes</th>
                            <th>LR (log<sub>10</sub>)</th>
                        </tr>
                        <#list sparkline as sprk>
                            <tr>
                                <td class="posttest">${sprk.rank}</td>
                                <td>${sprk.posttestBarSvg}</td>
                                <td><a href="#diagnosis${sprk.rank}">${sprk.diseaseName}</a></td>
                                <td>${sprk.diseaseAnchor}</td>
                                <td>${sprk.sparklineSvg}</td>
                                <td class="posttest">${sprk.compositeLikelihoodRatio}</td>
                            </tr>
                        </#list>
                    </table>
                <#else>
                     <table class="posttest">
                        <tr>
                            <th>Rank</th>
                            <th>Post-test probability</th>
                            <th>Disease</th>
                            <th>Phenotypes</th>
                            <th>Gene</th>
                            <th>LR (log<sub>10</sub>)</th>
                        </tr>
                        <#list sparkline as sprk>
                            <tr>
                                <td class="posttest">${sprk.rank}</td>
                                <td>${sprk.posttestBarSvg}</td>
                                <td><a href="#diagnosis${sprk.rank}">${sprk.diseaseName}</a></td>
                                <td>${sprk.sparklineSvg}</td>
                                <td class="posttest">${sprk.geneSparklineSvg}</td>
                                <td class="posttest">${sprk.compositeLikelihoodRatio}</td>
                            </tr>
                        </#list>
                    </table>
                </#if>
            </div>
        </article>
    </section>

    <#list differentialDiagnoses as dd>
        <section class="elevate-container">
            <article class="diff-dg">
                <a id="${dd.anchor}"></a>
                <header class="dg-header center">
                    <h2 class="dg-rank">Rank #${dd.rank}</h2>
                    <h2 class="dg-title">${dd.diseaseName}
                        <span class="dg-id">
                            <a href="${dd.url}" target="_blank">${dd.diseaseCurie}</a>
                        </span>
                    </h2>
                </header>

                <table class="redTable">
                    <tr>
                        <th>Pretest probability</th>
                        <th>Log<sub>10</sub> composite likelihood ratio</th>
                        <th>Posttest probability</th>
                    </tr>
                    <tr>
                        <td>${dd.pretestprob}</td>
                        <td>${dd.compositeLR}</td>
                        <td>${dd.posttestprob}</td>
                    </tr>
                </table>
                <br/>
                <div class="genotype">
                    <div class="genotype-head">
                        <#if dd.entrezGeneId != "n/a">
                        <h3>
                            <span class="hgvs-symbol">
                                ${dd.geneSymbol}
                                <span class="entrez-id">
                                    <a href="${dd.geneUrl}" target="_blank">
                                        ${dd.entrezGeneId}
                                    </a>
                                </span>
                            </span>
                        </h3>
                        </#if>
                        <#if dd.hasGenotypeExplanation()>
                            <p class="genotype-explanation">${dd.genotypeExplanation}</p>
                        </#if>
                    </div>
                    <div class="lr-contributions">
                        ${dd.svg}
                    </div>
                    <br>
                    <#if dd.hasVariants()>
                        <div class="genotype-body">
                            <table class="minimalistBlack table-${dd.geneSymbol}-${dd.diseaseCurie?replace(':','-')}">
                                <thead>
                                <tr>
                                    <th>Position</th>
                                    <th>Pathogenicity score</th>
                                    <th>Max. pop. frequency</th>
                                    <th>Genotype</th>
                                    <th>ClinVar</th>
                                    <th>Annotation</th>
                                </tr>
                                </thead>
                                <#list dd.visualizableVariants as svar>
                                    <tr>
                                        <td>${svar.ucsc}</td>
                                        <td>${svar.pathogenicityScore!"n/a"}</td>
                                        <td>${svar.frequency}%</td>
                                        <td>${svar.genotype}</td>
                                        <td>${svar.clinvar}</td>
                                        <td>
                                            <ul class="no-list-style">
                                                <#list svar.annotationList as annot>
                                                    <li <#if annot?index gt 2>class="extra-annotations extra-annotations-${svar.contigName()}-${svar.pos()?replace(',','')}"</#if>>
                                                        ${annot.accession!"n/a"}
                                                        : ${annot.hgvsCdna!"n/a"} ${annot.hgvsProtein!"n/a"} ${annot.variantEffect!"n/a"}
                                                    </li>
                                                </#list>
                                                <#if svar.annotationList?size gt 2>
                                                    <li><a class="extra-annotations-toggle"
                                                           id="extra-annotations-toggle-${svar.contigName()}-${svar.pos()?replace(',','')}"
                                                           onclick="toggleExtraAnnotations('${svar.contigName()}-${svar.pos()?replace(',','')}')">Show
                                                            All Annotations</a></li>
                                                </#if>
                                            </ul>
                                        </td>
                                    </tr>
                                </#list>
                            </table>
                        </div>
                    </#if>
                </div>
            </article>
        </section>
    </#list>

    <section class="elevate-container">
        <a id="othergenes"></a>
        <article class="othergenes-table">
            <h2>Low Post-Test Probability Results</h2>
            <p>Variants were identified in the following genes. The posttest probability of diseases
                associated with these genes was below ${postprobthreshold}. The table shows the total count of
                variants found in the genes.</p>
            <a id="show-other-genes-table" class="table-btn" onclick="showTable()">Show Table</a>
            <a id="hide-other-genes-table" class="table-btn" onclick="hideTable()">Hide Table</a>
            <table class="redTable" id="other-genes-table">
                <tr>
                    <th style="width:60%">Disease</th>
                    <th>Gene</th>
                    <th>Post test probability</th>
                    <th>variant count</th>
                </tr>
                <#list improbableDifferentials as ipd>
                    <tr>
                        <td class="disease"><a href="https://omim.org/${ipd.diseaseId}"
                                               target="_blank">${ipd.diseaseName}</a></td>
                        <td><i>${ipd.geneName}</i></td>
                        <td>${ipd.posttestProbability}</td>
                        <td>${ipd.varcount}</td>
                    </tr>
                </#list>
            </table>
        </article>
    </section>

    <#if geneSymbolsWithoutIds?has_content>
        <section>
            <article>
                <h2>Gene symbols that could not be annotated</h2>
                <p>LIRICAL relates variants to genes and diseases by means of the NCBI Gene ID. In some cases, gene
                    symbols
                    returned from the VCF annotation cannot be mapped to a Gene ID. We have observed that these are
                    often
                    accession numbers of poorly defined entities. The following list shows gene IDs that could not
                    be identified in this run. If there are many entries in this list, we recommend trying a different
                    annotation source (e.g., RefSeq). See also the online documentation of LIRICAL.</p>
                <a id="show-symbol-table" class="table-btn" onclick="showSymbolTable()">Show Table</a>
                <a id="hide-symbol-table" class="table-btn" onclick="hideSymbolTable()">Hide Table</a>
                <table class="redTable" id="symbol-table">
                    <tr>
                        <th style="width:20%">Symbol</th>
                    </tr>
                    <#list geneSymbolsWithoutIds as sym>
                        <tr>
                            <td class="disease">${sym}</td>
                        </tr>
                    </#list>
                </table>
            </article>
        </section>
    </#if>


    <section>
        <a id="explain"></a>
        <article>
            <h2>Definitions</h2>
            <p>LIRICAL calculates likelihood ratios for each HPO feature and for the genotype (if applicable).
                It displays detailed information for the top differential diagnoses (by default all diseases
                with a posttest probability above ${postprobthreshold} and at least 5; these thresholds can be
                adjusted if desired). The following text provides brief explanations of the symbols used by
                LIRICAL to explain how phenotype likelihood ratio scores were generated.</p>
            <ul class="no-list-style">
                <li><b>E</b>: Exact match between query term and disease term.</li>
                <li><b>Q&lt;D</b>: Query term is a child of disease term.</li>
                <li><b>D&lt;Q</b>: Disease term is child of query term.</li>
                <li><b>Q~D</b>: Query term and disease term are related but are separated by more than one edge.</li>
                <li><b>NM</b>: No common ancestor match besides the root of the ontology.</li>
                <li><b>X</b>: Query term is explicitly annotated as being not present in disease</li>
                <li><b>XX</b>: Term excluded by query and explicitly annotated as being not present in disease</li>
                <li><b>XA</b>: Term excluded by query and not explicitly annotated as being present in disease</li>
                <li><b>XP</b>: Term excluded by query but is explicitly annotated as being present in disease</li>
                <li><b>U</b>: Flag for unusual background query (please report to developers)</li>
            </ul>
        </article>
    </section>
    <section>
        <a id="about"></a>
        <article>
            <h2>About</h2>
            <p>LIRICAL is a tool for exploring exome or genome sequencing data obtained for an individual with suspected
                rare genetic disease.
                LIRICAL uses phenotypic features that describe the clinical manifestations observed in the individual
                and expressed
                as <a href="http://www.human-phenotype-ontology.org">Human Phenotype Ontology</a> (HPO) terms as well as
                the
                sequence variants found in the exome or genome file to derive a list of candidate diagnoses with
                estimated posterior
                probabilities. LIRICAL is intended as a resource to aide diagnosticians and does not make a diagnosis
                itself. The
                results of LIRICAL should not be construed as medical advice and should always be reviewed by medical
                professionals.</p>
            <p>See LIRICAL's <a href="https://thejacksonlaboratory.github.io/LIRICAL/stable" target="_blank">online
                    documentation</a>.</p>

            <h4><i>This LIRICAL run had the following configuration:</i></h4>
            <ul>
                <#if resultsMeta.liricalPath?has_content>
                    <li>Path to LIRICAL data directory: ${resultsMeta.liricalPath}</li>
                </#if>
                <#if resultsMeta.hpoVersion?has_content>
                    <li>Human Phenotype Ontology version: ${resultsMeta.hpoVersion}</li>
                </#if>
                <#if resultsMeta.globalMode?has_content>
                    <li>Global analysis mode: ${resultsMeta.globalMode?string("Yes", "No")}</li>
                </#if>
                <#if !phenotypeOnly>
                    <#if resultsMeta.transcriptDatabase?has_content>
                        <li>Transcript database: ${resultsMeta.transcriptDatabase}</li>
                    </#if>
                    <#if resultsMeta.exomiserPath?has_content>
                        <li>Path to Exomiser database file: ${resultsMeta.exomiserPath}</li>
                    </#if>
                    <#if resultsMeta.nPassingVariants?has_content>
                        <li>Good quality variants: ${resultsMeta.nPassingVariants}</li>
                    </#if>
                    <#if resultsMeta.nFilteredVariants?has_content>
                        <li>Variants removed due to failing quality filter: ${resultsMeta.nFilteredVariants}</li>
                    </#if>
                    <#if resultsMeta.genesWithVar?has_content>
                        <li>Genes found to have at least one variant: ${resultsMeta.genesWithVar}</li>
                    </#if>
                </#if>
            </ul>
            </p>
        </article>
    </section>
    <span id="tooltip" style="position: absolute; display: none;"></span>
</main>
<footer>
    <p>LIRICAL ${resultsMeta.liricalVersion!""} &copy; 2023</p>
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

    function showSymbolTable() {
        var table = document.getElementById("symbol-table");
        table.style.display = "block";
        var showtablebtn = document.getElementById("show-symbol-table");
        showtablebtn.style.display = "none";

        var hidetablebtn = document.getElementById("hide-symbol-table");
        hidetablebtn.style.display = "block";
    }

    function hideSymbolTable() {
        var table = document.getElementById("symbol-table");
        table.style.display = "none";
        var showtablebtn = document.getElementById("show-symbol-table");
        showtablebtn.style.display = "block";

        var hidetablebtn = document.getElementById("hide-symbol-table");
        hidetablebtn.style.display = "none";
    }

    function toggleExtraAnnotations(uniqueId) {
        const targetClass = '.extra-annotations-' + uniqueId
        const elements = document.querySelectorAll(targetClass);
        let showing = false;
        for (var i = 0; i < elements.length; ++i) {
            var row = elements[i];
            if (row.style.display === 'none' || row.style.display === '') {
                showing = true;
                row.style.display = 'list-item';
            } else {
                row.style.display = 'none';
            }
        }
        const btn = document.getElementById('extra-annotations-toggle-' + uniqueId);
        if (showing) {
            btn.innerText = 'Hide Annotations';
        } else {
            btn.innerText = 'Show All Annotations';
        }
    }

</script>
</body>
</html>

! LR2PG TSV Output
! Sample: ${sample_name}
! Observed HPO terms
<#list  observedHPOs as hpo>
! ${hpo}
</#list>
${header}
<#list diff as dd>
${dd.rank}\t${dd.diseaseName}\t${dd.diseaseCurie}\t${dd.pretestprob}\t${dd.posttestprob}\t${dd.compositeLR}\t${dd.entrezGeneId}\t${dd.varString}
</#list>
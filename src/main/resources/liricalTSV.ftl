! LR2PG TSV Output
! Sample: ${sample_name!"n/a"}
! Observed HPO terms
<#assign tab="\t">
<#list  observedHPOs as hpo>
! ${hpo}
</#list>
${header}
<#list diff as dd>
${dd.rank}${tab}${dd.diseaseName}${tab}${dd.diseaseCurie}${tab}${dd.pretestprob}${tab}${dd.posttestprob}${tab}${dd.compositeLR}${tab}${dd.entrezGeneId}${tab}${dd.varString}
</#list>
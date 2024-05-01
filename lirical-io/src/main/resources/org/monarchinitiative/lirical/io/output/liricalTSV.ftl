! LIRICAL TSV Output (${resultsMeta.liricalVersion})
! Sample: ${resultsMeta.sampleName!"n/a"}
<#if observedHPOs?has_content>
! Observed HPO terms:
<#list observedHPOs as obs_hpo>
!  ${obs_hpo}
</#list>
</#if>
<#if excludedHPOs?has_content>
! Excluded HPO terms:
<#list excludedHPOs as ex_hpo>
!  ${ex_hpo}
</#list>
</#if>
<#assign tab="\t">
<#if phenotypeOnly>
rank${tab}diseaseName${tab}diseaseCurie${tab}pretestprob${tab}posttestprob${tab}compositeLR
<#list diff as dd>
${dd.rank}${tab}${dd.diseaseName}${tab}${dd.diseaseCurie}${tab}${dd.pretestprob}${tab}${dd.posttestprob}${tab}${dd.compositeLR}
</#list>
<#else>
rank${tab}diseaseName${tab}diseaseCurie${tab}pretestprob${tab}posttestprob${tab}compositeLR${tab}entrezGeneId${tab}variants
<#list diff as dd>
${dd.rank}${tab}${dd.diseaseName}${tab}${dd.diseaseCurie}${tab}${dd.pretestprob}${tab}${dd.posttestprob}${tab}${dd.compositeLR}${tab}${dd.entrezGeneId}${tab}${dd.varString}
</#list>
</#if>

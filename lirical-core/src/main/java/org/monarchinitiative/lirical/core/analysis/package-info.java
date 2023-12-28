/**
 * A high-level representation of LIRICAL analysis.
 * <p>
 * The analysis subject is provided as {@link org.monarchinitiative.lirical.core.analysis.AnalysisData}. The analysis
 * is parameterized by {@link org.monarchinitiative.lirical.core.analysis.AnalysisOptions}.
 * {@link org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner} executes the analysis. The output
 * are wrapped into {@link org.monarchinitiative.lirical.core.analysis.AnalysisResults} which reports results
 * of matching the subject to computational disease models,
 * one {@link org.monarchinitiative.lirical.core.analysis.TestResult} per disease.
 */
package org.monarchinitiative.lirical.core.analysis;
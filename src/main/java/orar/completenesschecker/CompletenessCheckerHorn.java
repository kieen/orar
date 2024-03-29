package orar.completenesschecker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

import orar.config.Configuration;
import orar.config.LogInfo;
import orar.dlreasoner.DLReasoner;
import orar.materializer.Materializer;
import orar.modeling.ontology.AssertionDecoder;
import orar.modeling.ontology.OrarOntology;
import orar.util.PrintingHelper;

public class CompletenessCheckerHorn implements CompletenessChecker {
	private static Logger logger = Logger.getLogger(CompletenessCheckerHorn.class);
	private Configuration config = Configuration.getInstance();
	private final Materializer materializer;
	private final DLReasoner owlRealizer;
	private boolean isEntailmentsComputed;

	private boolean isConceptCountCorrect;
	private boolean isRoleCountCorrect;
	private boolean isSameasCountCorrect; // should be correct as we don't
											// really generate owlapi sameas
											// assertions.

	// concept assertions
	private Set<OWLClassAssertionAxiom> entailedConceptAssertionByAbstraction;
	private Set<OWLClassAssertionAxiom> entailedConceptAssertionByDLReasoner;

	// role assertions
	private Set<OWLObjectPropertyAssertionAxiom> entailedRoleAssertionByAbstraction;
	private Set<OWLObjectPropertyAssertionAxiom> entailedRoleAssertionByDLReasoner;

	// sameas
	private Map<OWLNamedIndividual, Set<OWLNamedIndividual>> entailedSameasMapByAbstraction;

	private Map<OWLNamedIndividual, Set<OWLNamedIndividual>> entailedSameasMapByDLReasoner;

	public CompletenessCheckerHorn(Materializer materializer, DLReasoner owlRealizer) {
		this.materializer = materializer;
		this.owlRealizer = owlRealizer;
		this.isEntailmentsComputed = false;
	}

	@Override
	public void computeEntailments() {

		/*
		 * get result by abstraction
		 */
		long startTimeAbstraction = System.currentTimeMillis();
		materializer.materialize();
		long endTimeAbstraction = System.currentTimeMillis();
		long timeByAbstraction = (endTimeAbstraction - startTimeAbstraction) / 1000;
		logger.info("Time by abstraction refinement (s):" + timeByAbstraction);

		OrarOntology orarOntology = materializer.getOrarOntology();

		this.entailedConceptAssertionByAbstraction = orarOntology
				.getOWLAPIConceptAssertionsWithoutNormalizationSymbolsTakingSAMEASIntoAccount();

		this.isConceptCountCorrect = (this.entailedConceptAssertionByAbstraction.size() == orarOntology
				.getNumberOfConceptAssertionsWithoutNormalizationSymbolsTakingSAMEASIntoAccount());

		this.entailedRoleAssertionByAbstraction = orarOntology.getOWLAPIRoleAssertionsTakingSAMEASIntoAccount();

		this.isRoleCountCorrect = (this.entailedRoleAssertionByAbstraction.size() == orarOntology
				.getNumberOfRoleAssertionsTakingSAMEASIntoAccount());

		this.entailedSameasMapByAbstraction = AssertionDecoder
				.getSameasMapInOWLAPI(orarOntology.getEntailedSameasAssertions());

		logger.info("Number of derived concept assertions by abstraction materializer:"
				+ entailedConceptAssertionByAbstraction.size());
		logger.info("Number of derived role assertions by abstraction materializer:"
				+ entailedRoleAssertionByAbstraction.size());
		/*
		 * get result by owlrealizer
		 */
		long startTimeDLReasoner = System.currentTimeMillis();
		owlRealizer.computeEntailments();
		long endTimeDLReasoner = System.currentTimeMillis();
		long timeByDLReasoner = (endTimeDLReasoner - startTimeDLReasoner) / 1000;
		logger.info("Time by a DL Reasoner (s):" + timeByDLReasoner);
		this.entailedConceptAssertionByDLReasoner = owlRealizer.getEntailedConceptAssertions();
		this.entailedRoleAssertionByDLReasoner = owlRealizer.getEntailedRoleAssertions();
		// logger.info("Entialed role assertions by DL reasoner:");
		// PrintingHelper.printSet(this.entailedRoleAssertionByDLReasoner);
		this.entailedSameasMapByDLReasoner = owlRealizer.getEntailedSameasAssertions();

		/*
		 * indicate that entailments have been computed.
		 */
		this.isEntailmentsComputed = true;
	}

	// @Override
	// public boolean isRoleAssertionComplete() {
	// if (!isEntailmentsComputed) {
	// computeEntailments();
	// }
	// logger.info("Number of derived role assertions by abstraction
	// materializer:"
	// + entailedRoleAssertionByAbstraction.size());
	//// PrintingHelper.printSet(logger,this.entailedRoleAssertionByAbstraction);
	// logger.info("Number of derived role assertions by OWL reasoner:" +
	// entailedRoleAssertionByDLReasoner.size());
	//// PrintingHelper.printSet(logger,this.entailedRoleAssertionByDLReasoner);
	// SetView<OWLObjectPropertyAssertionAxiom> difference =
	// Sets.difference(this.entailedRoleAssertionByAbstraction,
	// this.entailedRoleAssertionByDLReasoner);
	//
	// if (config.getLogInfos().contains(LogInfo.COMPARED_RESULT_INFO)) {
	// if (!difference.isEmpty()) {
	//
	// logger.info("========Role asesrtions by abstraction but not by
	// OWLRealizer=============");
	// PrintingHelper.printSet(logger, Sets.intersection(difference,
	// this.entailedRoleAssertionByAbstraction));
	//
	// logger.info("========Role assertions by OWLRealizer but not by
	// abstraction=============");
	// PrintingHelper.printSet(logger, Sets.intersection(difference,
	// this.entailedRoleAssertionByDLReasoner));
	//
	// }
	// }
	//// return difference.isEmpty();
	// return
	// this.entailedRoleAssertionByAbstraction.equals(this.entailedRoleAssertionByDLReasoner);
	// }
	//

	@Override
	public boolean isRoleAssertionComplete() {
		if (!isEntailmentsComputed) {
			computeEntailments();
		}
		boolean isComplete = this.entailedRoleAssertionByAbstraction.equals(this.entailedRoleAssertionByDLReasoner);
		logger.info("Number of derived role assertions by abstraction materializer:"
				+ entailedRoleAssertionByAbstraction.size());
		// PrintingHelper.printSet(logger,this.entailedRoleAssertionByAbstraction);
		logger.info("Number of derived role assertions by OWL reasoner:" + entailedRoleAssertionByDLReasoner.size());
		// PrintingHelper.printSet(logger,this.entailedRoleAssertionByDLReasoner);

		if (config.getLogInfos().contains(LogInfo.COMPARED_RESULT_INFO)) {
			if (!isComplete) {

				HashSet<OWLObjectPropertyAssertionAxiom> copyOfResultByAbstraction = new HashSet<OWLObjectPropertyAssertionAxiom>(
						this.entailedRoleAssertionByAbstraction);

				HashSet<OWLObjectPropertyAssertionAxiom> copyOfResultByOWLRealizer = new HashSet<OWLObjectPropertyAssertionAxiom>(
						this.entailedRoleAssertionByDLReasoner);

				copyOfResultByAbstraction.removeAll(this.entailedRoleAssertionByDLReasoner);
				copyOfResultByOWLRealizer.removeAll(this.entailedRoleAssertionByAbstraction);
				logger.info("========Role asesrtions by abstraction but not by OWLRealizer=============");
				PrintingHelper.printSet(copyOfResultByAbstraction);

				logger.info("========Role assertions by OWLRealizer but not by abstraction=============");
				PrintingHelper.printSet(copyOfResultByOWLRealizer);

			}
		}
		// return difference.isEmpty();
		return this.entailedRoleAssertionByAbstraction.equals(this.entailedRoleAssertionByDLReasoner);
	}

	@Override
	public boolean isSameasComplete() {
		if (!isEntailmentsComputed) {
			computeEntailments();
		}

		// boolean equal =
		// MapComparator.isEqual(this.entailedSameasMapByAbstraction,
		// this.entailedSameasMapByDLReasoner);
		boolean equal = this.entailedSameasMapByAbstraction.equals(this.entailedSameasMapByDLReasoner);

		if (!equal) {
			logger.info("sameas map by Abstraction:");
			PrintingHelper.printMap(this.entailedSameasMapByAbstraction);
			logger.info("sameas map by a DL reasoner:");
			PrintingHelper.printMap(this.entailedSameasMapByDLReasoner);
		}
		return equal;
		// MapDifference<OWLNamedIndividual, Set<OWLNamedIndividual>> difference
		// = Maps
		// .difference(this.entailedSameasMapByAbstraction,
		// this.entailedSameasMapByDLReasoner);
		//
		// if (config.getLogInfos().contains(LogInfo.COMPARED_RESULT_INFO)) {
		// if (!difference.areEqual()) {
		// logger.info("===Sameas by abstraction===");
		// PrintingHelper.printMap(this.entailedSameasMapByAbstraction);
		//
		// logger.info("========Sameas asesrtions by abstraction but not by
		// OWLRealizer=============");
		// PrintingHelper.printMap(logger,difference.entriesOnlyOnLeft());
		//
		//
		// logger.info("===Sameas by OWLReasoner===");
		// PrintingHelper.printMap(this.entailedSameasMapByDLReasoner);
		//
		// logger.info("========Sameas asesrtions by OWLRealizer but not by
		// abstraction=============");
		// PrintingHelper.printMap(logger,difference.entriesOnlyOnRight());
		// }
		// }
		// return difference.areEqual();
	}

	@Override
	public boolean isConceptAssertionComplete() {
		if (!isEntailmentsComputed) {
			computeEntailments();
		}
		logger.info("Number of derived concept assertions by abstraction materializer:"
				+ entailedConceptAssertionByAbstraction.size());

		logger.info(
				"Number of derived concept assertions by OWL reasoner:" + entailedConceptAssertionByDLReasoner.size());

		boolean iscomplete = entailedConceptAssertionByAbstraction.equals(this.entailedConceptAssertionByDLReasoner);

		if (config.getLogInfos().contains(LogInfo.COMPARED_RESULT_INFO)) {
			if (!iscomplete) {

				HashSet<OWLClassAssertionAxiom> copyOfResultByAbstraction = new HashSet<OWLClassAssertionAxiom>(
						entailedConceptAssertionByAbstraction);

				HashSet<OWLClassAssertionAxiom> copyOfResultByOWLRealizer = new HashSet<OWLClassAssertionAxiom>(
						entailedConceptAssertionByDLReasoner);

				copyOfResultByAbstraction.removeAll(entailedConceptAssertionByDLReasoner);
				copyOfResultByOWLRealizer.removeAll(entailedConceptAssertionByAbstraction);

				logger.info("========Concept asesrtions by abstraction but not by OWLRealizer=============");
				PrintingHelper.printSet(logger, copyOfResultByAbstraction);

				logger.info("========Concept assertions by OWLRealizer but not by abstraction=============");
				PrintingHelper.printSet(logger, copyOfResultByOWLRealizer);

			}

		}
		return iscomplete;
	}

	@Override
	public boolean isCountingAssertionCorrect() {

		return this.isConceptCountCorrect && this.isRoleCountCorrect;
	}

	// @Override
	// public boolean isConceptAssertionComplete() {
	// if (!isEntailmentsComputed) {
	// computeEntailments();
	// }
	// logger.info("Number of derived concept assertions by abstraction
	// materializer:"
	// + entailedConceptAssertionByAbstraction.size());
	//
	// logger.info(
	// "Number of derived concept assertions by OWL reasoner:" +
	// entailedConceptAssertionByDLReasoner.size());
	//
	// SetView<OWLClassAssertionAxiom> difference =
	// Sets.difference(entailedConceptAssertionByAbstraction,
	// this.entailedConceptAssertionByDLReasoner);
	//
	// if (config.getLogInfos().contains(LogInfo.COMPARED_RESULT_INFO)) {
	// if (!difference.isEmpty()) {
	//
	// logger.info("========Concept asesrtions by abstraction but not by
	// OWLRealizer=============");
	// PrintingHelper.printSet(logger,
	// Sets.intersection(difference,
	// this.entailedConceptAssertionByAbstraction));
	//
	// logger.info("========Concept assertions by OWLRealizer but not by
	// abstraction=============");
	// PrintingHelper.printSet(logger,
	// Sets.intersection(difference,
	// this.entailedConceptAssertionByDLReasoner));
	//
	// }
	//
	// }
	// return difference.isEmpty();
	// }

}

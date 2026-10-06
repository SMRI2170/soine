package app.soine.dream

import app.soine.relationship.CompanionProgressRepository
import app.soine.sleep.SleepSessionRecord
import app.soine.sleep.SleepSessionStatus

class DreamDiscoveryCoordinator(
    private val repository: DreamDiscoveryRepository,
    private val relationshipRepository: CompanionProgressRepository,
    private val engine: DeterministicDreamDiscoveryEngine =
        DeterministicDreamDiscoveryEngine(InitialDreamCatalog.repository),
) {
    suspend fun discoveries(): List<DreamDiscovery> =
        repository.get().discoveries

    suspend fun evaluateIfNeeded(
        session: SleepSessionRecord,
        season: DreamSeason? = null,
    ): DreamDiscoveryOutcome {
        require(session.status == SleepSessionStatus.COMPLETED) {
            "Only completed sleep sessions can be evaluated for dreams."
        }

        val snapshot = repository.get()
        val relationship = relationshipRepository.get()
        val outcome = engine.evaluate(
            DreamDiscoveryInput(
                session = session,
                relationship = relationship,
                season = season,
                existingDiscoveries = snapshot.discoveries,
                priorDecisions = snapshot.decisions,
            )
        )

        if (outcome is DreamDiscoveryOutcome.Evaluated) {
            repository.saveEvaluation(
                decision = outcome.decision,
                discovery = outcome.discovery,
            )
        }
        return outcome
    }
}

package io.zerobase.smarttracing.api.features.devices

import com.google.inject.Inject
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.api.EntityCreationException
import io.zerobase.smarttracing.api.InvalidIdException
import io.zerobase.smarttracing.api.gremlin.execute
import io.zerobase.smarttracing.api.gremlin.getIfPresent
import io.zerobase.smarttracing.api.now
import io.zerobase.smarttracing.common.LoggerDelegate
import io.zerobase.smarttracing.common.models.*
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.set
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality.single
import java.util.*
import java.util.UUID.randomUUID

class DevicesDao @Inject constructor(private val graph: GraphTraversalSource) {
    companion object {
        private val log by LoggerDelegate()
    }

    /**
     * Creates a new Device and returns its ID
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createDevice(fingerprint: String?): String {
        val id = randomUUID().toString()
        try {
            val vertex = graph.addV("Device")
                .property(T.id, id)
                .property("fingerprint", fingerprint ?: "none")
                .property("creationTimestamp", now())
                .execute()
            return vertex?.run { id } ?: throw EntityCreationException("Failed to save device")
        } catch (ex: Exception) {
            log.error("error creating device. fingerprint={}", fingerprint, ex)
            throw EntityCreationException("Error creating device", ex)
        }
    }

    /**
     * Creates a new CheckIn and returns its ID
     */
    fun createCheckIn(deviceId: String, scannedId: String, loc: Location?): String {
        val scanId = randomUUID().toString()
        try {
            val deviceNode = graph.V(deviceId).getIfPresent() ?: throw InvalidIdException(deviceId)
            val scannableNode = graph.V(scannedId).hasLabel("Scannable").getIfPresent() ?: throw InvalidIdException(scannedId)
            val traversal = graph.addE("SCAN")
                .from(deviceNode)
                .to(scannableNode)
                .property(T.id, scanId)
                .property("timestamp", now())
            loc?.also { (lat, long) -> traversal.property("latitude", lat).property("longitude", long) }
            traversal.execute()
            return scanId
        } catch (ex: Exception) {
            log.error("error creating check-in. device={} scannable={}", deviceId, scannedId, ex)
            throw EntityCreationException("Error creating check-in", ex)
        }
    }

    /**
     * Updates the location attribute of a CheckIn. Throws 404 if CheckIn doesn't exist.
     */
    fun updateCheckInLocation(deviceId: String, checkInId: String, loc: Location) {
        graph.V(checkInId).inE("SCAN")
            .from(graph.V(deviceId))
            .property("latitude", loc.latitude)
            .property("longitude", loc.longitude)
            .execute()
    }

    fun recordPeerToPeerScan(scanner: String, scanned: String, loc: Location?): String {
        val scanId = randomUUID().toString()
        try {
            val aNode = graph.V(scanner).getIfPresent() ?: throw InvalidIdException(scanner)
            val bNode = graph.V(scanned).getIfPresent() ?: throw InvalidIdException(scanned)
            graph.addE("SCAN")
                .from(aNode)
                .to(bNode)
                .property(T.id, scanId)
                .property("timestamp", now())
                .property("latitude", loc?.latitude ?: 0)
                .property("longitude", loc?.longitude ?: 0)
                .execute()
            return scanId
        } catch (ex: Exception) {
            log.error("error creating p2p scan. scanner={} scanned={}", scanner, scanned, ex)
            throw EntityCreationException("Error creating scan relationship between devices.", ex)
        }
    }

    fun recordTestResult(testResult: TestResult): String {
        val reportId = randomUUID().toString()
        try {
            val deviceNode = graph.V(testResult.testedParty).getIfPresent() ?: throw InvalidIdException(testResult.testedParty)
            val reporterNode = graph.V(testResult.reportedBy).getIfPresent() ?: throw InvalidIdException(testResult.reportedBy)
            graph.addV("TestResult").
                property(T.id, reportId).
                property(single, "verified", testResult.verified).
                property(single, "testDate", testResult.testDate.toString()).
                property(single, "result", testResult.result).
                property(single, "timestamp", Date.from(testResult.timestamp)).
                addE("REPORTED").property(single, "timestamp", Date.from(testResult.timestamp)).from(reporterNode).
                // go back to the report node to make the second edge
                inV().addE("FOR").to(deviceNode).
                execute()
            return reportId
        } catch (ex: Exception) {
            log.error("error recording test result. device={} verified={}", testResult.testedParty, testResult.verified, ex)
            throw EntityCreationException("Error recording test result.", ex)
        }
    }

    @SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION", justification = "false positive because kotlin")
    fun recordSymptoms(data: SymptomSummary): String {
        val reportId = randomUUID().toString()
        try {
            val deviceNode = graph.V(data.testedParty).getIfPresent() ?: throw InvalidIdException(data.testedParty)
            val reporterNode = graph.V(data.reportedBy).getIfPresent() ?: throw InvalidIdException(data.reportedBy)
            val traversal = graph.addV("Symptoms").
                property(T.id, reportId).
                property(single, "verified", data.verified).
                property(single, "timestamp", Date.from(data.timestamp)).
                property(set, "symptoms", data.symptoms.map(Symptom::name))
            data.temperature?.toCelsius()?.also { traversal.property(single, "temperature", it) }
            data.age?.also { traversal.property(single, "age", it.name) }
            data.householdSize?.also { traversal.property(single, "householdSize", it.name) }
            data.publicInteractionScale?.also { traversal.property(single, "publicInteractionScale", it.name) }
            traversal.addE("REPORTED").property(single, "timestamp", data.timestamp).from(reporterNode).
            // go back to the report node to make the second edge
            inV().addE("REPORT_FOR").to(deviceNode).
            execute()
            return reportId
        } catch (ex: Exception) {
            log.error("error recording symptoms. device={} verified={}", data.testedParty, data.verified, ex)
            throw EntityCreationException("Error recording symptoms.", ex)
        }
    }
}

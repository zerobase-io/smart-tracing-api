package io.zerobase.smarttracing.features.devices

import com.google.inject.Inject
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.gremlin.execute
import io.zerobase.smarttracing.gremlin.getIfPresent
import io.zerobase.smarttracing.models.*
import io.zerobase.smarttracing.utils.LoggerDelegate
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import java.util.*

class DevicesDao @Inject constructor(private val graph: GraphTraversalSource) {
    companion object {
        private val log by LoggerDelegate()
    }

    /**
     * Creates a new Device and returns its ID
     */
    @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
    fun createDevice(fingerprint: Fingerprint?): DeviceId {
        val id = UUID.randomUUID().toString()
        try {
            val vertex = graph.addV("Device")
                .property(T.id, id)
                .property("fingerprint", fingerprint?.value ?: "none")
                .property("creationTimestamp", System.currentTimeMillis())
                .execute()
            return vertex?.run { DeviceId(id) } ?: throw EntityCreationException("Failed to save device")
        } catch (ex: Exception) {
            log.error("error creating device. fingerprint={}", fingerprint, ex)
            throw EntityCreationException("Error creating device", ex)
        }
    }

    /**
     * Creates a new CheckIn and returns its ID
     */
    fun createCheckIn(deviceId: DeviceId, scannedId: ScannableId, loc: Location?): ScanId {
        val scanId = UUID.randomUUID().toString()
        try {
            val deviceNode = graph.V(deviceId.value).getIfPresent() ?: throw InvalidIdException(deviceId)
            val scannableNode = graph.V(scannedId.value).hasLabel("Scannable").getIfPresent() ?: throw InvalidIdException(scannedId)
            val traversal = graph.addE("SCAN")
                .from(deviceNode)
                .to(scannableNode)
                .property(T.id, scanId)
                .property("timestamp", System.currentTimeMillis())
            loc?.also { (lat, long) -> traversal.property("latitude", lat).property("longitude", long) }
            traversal.execute()
            return ScanId(scanId)
        } catch (ex: Exception) {
            log.error("error creating check-in. device={} scannable={}", deviceId, scannedId, ex)
            throw EntityCreationException("Error creating check-in", ex)
        }
    }

    /**
     * Updates the location attribute of a CheckIn. Throws 404 if CheckIn doesn't exist.
     */
    fun updateCheckInLocation(deviceId: DeviceId, checkInId: ScanId, loc: Location) {
        graph.V(checkInId.value).inE("SCAN")
            .from(graph.V(deviceId.value))
            .property("latitude", loc.latitude)
            .property("longitude", loc.longitude)
            .execute()
    }

    fun recordPeerToPeerScan(scanner: DeviceId, scanned: DeviceId, loc: Location?): ScanId {
        val scanId = UUID.randomUUID().toString()
        try {
            val aNode = graph.V(scanner.value).getIfPresent() ?: throw InvalidIdException(scanner)
            val bNode = graph.V(scanned.value).getIfPresent() ?: throw InvalidIdException(scanned)
            graph.addE("SCAN")
                .from(aNode)
                .to(bNode)
                .property(T.id, scanId)
                .property("timestamp", System.currentTimeMillis())
                .property("latitude", loc?.latitude ?: 0)
                .property("longitude", loc?.longitude ?: 0)
                .execute()
            return ScanId(scanId)
        } catch (ex: Exception) {
            log.error("error creating p2p scan. scanner={} scanned={}", scanner, scanned, ex)
            throw EntityCreationException("Error creating scan relationship between devices.", ex)
        }
    }
}

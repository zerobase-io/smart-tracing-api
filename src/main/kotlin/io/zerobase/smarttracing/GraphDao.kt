package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.PhoneNumberUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.models.SiteResponse
import io.zerobase.smarttracing.models.DeviceId
import io.zerobase.smarttracing.models.Fingerprint
import io.zerobase.smarttracing.models.ScanId
import io.zerobase.smarttracing.models.SiteId
import io.zerobase.smarttracing.models.ScannableId
import io.zerobase.smarttracing.models.OrganizationId
import org.neo4j.driver.Driver
import java.util.*

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
class GraphDao(private val driver: Driver, val phoneUtil: PhoneNumberUtil) {

    fun createDevice(fingerprint: Fingerprint?, ip: String?): DeviceId? {
        return driver.session().use { session ->
            session.writeTransaction { txn ->
                val result = txn.run(
                        "CREATE (d:Device {id: '${UUID.randomUUID()}', fingerprint: '${fingerprint?.value ?: "none"}', initialIp: '${ip ?: "none"}'}) RETURN d.id as id"
                ).single()["id"].asString()
                return@writeTransaction result?.let { DeviceId(it) }
            }
        }
    }

    fun recordPeerToPeerScan(scanner: DeviceId, scanned: DeviceId): ScanId? {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                            MATCH (d:Device) WHERE ID(d) = ${scanner.value}
                            MATCH (d2:Device) WHERE ID(d2) = ${scanned.value}
                            CREATE (d)-[r:SCAN{id: '${UUID.randomUUID()}', timestamp: TIMESTAMP()}]->(d2)
                            RETURN r.id as id
                        """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { ScanId(it) }
            }
        }
    }

    fun recordDeviceCheckIn(device: DeviceId, site: SiteId): ScanId? {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                            MATCH (d:Device) WHERE ID(d) = ${device.value}
                            MATCH (s:Site) WHERE ID(s) = ${site.value}
                            CREATE (d)-[r:SCAN{id: '${UUID.randomUUID()}', timestamp: TIMESTAMP()}]->(s)
                            RETURN r.id as id
                        """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { ScanId(it) }
            }
        }
    }

    /**
     * Creates an organization node inside neo4j.
     *
     * @param organization name of the organization that is being created.
     * @param phone phone number for the contact for the organization.
     * @param email email for the contact for the organization.
     * @param contactName name of the contact for the organization.
     * @param address address of the organization.
     * @param testing if the organization will be doing testing.
     * @param multiSite reporting if the organization has multiple sites.
     *
     * @return organization id.
     */
    fun createOrganization(organization: String, phone: String, email: String,
                           contactName: String, address: String, testing: Boolean,
                           multiSite: Boolean): OrganizationId? {
        val validNum = phoneUtil.isValidNumber(phoneUtil.parseAndKeepRawInput(phone, "US"))

        if (!validNum) {
            return null
        }

        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        CREATE (o:Organization {
                                    id: '${UUID.randomUUID()}',
                                    phone: '${phone}',
                                    email: '${email}',
                                    contactname: '${contactName}',
                                    organization: '${organization}',
                                    address: '${address}',
                                    verified: 'false',
                                    testing: '${testing}',
                                    multisite: '${multiSite}'
                        })
                        RETURN o.id as id
                        """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { OrganizationId(it) }
            }
        }
    }

    /**
     * Sets the multi-site flag in an organization.
     *
     * @param id organization uuid.
     * @param state the value for the multi-site flag
     */
    fun setMultiSite(id: String, state: Boolean) {
        driver.session().use {
            it.writeTransaction { txn ->
                txn.run(
                        """
                        MATCH (o:Organization { id: '${id}' })
                        SET o.multisite = 'true'
                        """
                )
            }
        }
    }

    /**
     * Creates site.
     *
     * @param id organization id.
     * @param name name of the site.
     * @param category category of the site.
     * @param subcategory subcategory of the site.
     * @param lat latitude of the site.
     * @param long longitude of the site.
     * @param testing if the site is testing or not.
     * @param phone contact phone of site manager
     * @param email contact email of site manager
     * @param contactName contact name of site manager
     */
    fun createSite(id: String, name: String, category: String,
                   subcategory: String, lat: Float, long: Float,
                   testing: Boolean, phone: String?, email: String?,
                   contactName: String?): SiteId? {
        val query = if (phone == null) {
            """
            MATCH (o:Organization { id: '${id}' })
            CREATE (s:Site {
                        id: '${UUID.randomUUID()}',
                        phone: o.phone,
                        email: o.email,
                        contactName: o.contactName,
                        name: '${name}',
                        category: '${category}',
                        subcategory: '${subcategory}',
                        latitude: '${lat}',
                        longitude: '${long}',
                        testing: '${testing}'
            })
            CREATE (o)-[r:OWNS]->(s)
            RETURN s.id as id
            """.trimIndent()
        } else {
             """
            MATCH (o:Organization { id: '${id}' })
            CREATE (s:Site {
                        id: '${UUID.randomUUID()}',
                        phone: '${phone!!}',
                        email: '${email!!}',
                        contactName: '${contactName!!}',
                        name: '${name}',
                        category: '${category}',
                        subcategory: '${subcategory}',
                        latitude: '${lat}',
                        longitude: '${long}',
                        testing: '${testing}'
            })
            CREATE (o)-[r:OWNS]->(s)
            RETURN s.id as id
            """.trimIndent()
        }
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(query).single()["id"].asString()
                return@writeTransaction result?.let { SiteId(it) }
            }
        }
    }

    /**
     * Gets all the sites list
     *
     * @param id id of the organization
     */
    fun getSites(id: String): List<SiteResponse>? {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        MATCH (o:Organization { id: '${id}' })
                        - [:OWNS] -> (s:Site)
                        RETURN s.id AS id, s.name AS name
                        """.trimIndent()
                ).list { x -> SiteResponse(x["id"].asString(), x["name"].asString()) }
                return@writeTransaction result?.let { it }
            }
        }
    }

    /**
     * Creates a scannable for a site.
     *
     * @param oid organization id
     * @param sid site id
     * @param type type of scannable
     * @param singleUse if it is a single use scannable or not
     */
    fun createScannable(oid: String, sid: String, type: String,
                        singleUse: Boolean): ScannableId? {
         return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        MATCH (o:Organization { id: '${oid}' })
                        - [:OWNS] -> (s:Site { id: '${sid}' })
                        CREATE (scan:Scannable {
                                    id: '${UUID.randomUUID()}',
                                    type: '${type}',
                                    singleUse: '${singleUse}',
                                    active: 'true'
                        })
                        CREATE (s)-[r:OWNS]->(scan)
                        RETURN scan.id as id
                        """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { ScannableId(it) }
            }
        }
    }
}

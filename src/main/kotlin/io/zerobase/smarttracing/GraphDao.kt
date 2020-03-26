package io.zerobase.smarttracing

import com.google.i18n.phonenumbers.PhoneNumberUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.zerobase.smarttracing.models.*
import org.neo4j.driver.Driver
import java.util.*

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", justification = "false positive")
class GraphDao(private val driver: Driver, val phoneUtil: PhoneNumberUtil) {

    /**
     * Creates a new Device and returns its ID
     */
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

    /**
     * Creates a new CheckIn and returns its ID
     */
    fun createCheckIn(deviceId: DeviceId, scannedId: ScannableId, loc: Location?): ScanId? {
        return driver.session().use { session ->
            session.writeTransaction { txn ->
                val result = txn.run(
                    """
                    MATCH (d:Device { id: '${deviceId.value}' })
                    MATCH (s:Scannable { id: '${scannedId.value}' })
                    CREATE (d) - [r:SCAN { id: '${UUID.randomUUID()}', latitude: '${loc?.latitude}', longitude: '${loc?.longitude}' }] -> (s)
                    RETURN r.id AS id
                    """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { ScanId(it) }
            }
        }
    }

    /**
     * Updates the location attribute of a CheckIn. Throws 404 if CheckIn doesn't exist.
     */
    fun updateCheckInLocation(deviceId: DeviceId, checkInId: ScanId, loc: Location) {
        driver.session().use { session ->
            session.writeTransaction { txn ->
                txn.run(
                    """
                    MATCH (d:Device { id: '${deviceId.value}' }) - [r:SCAN { id: '${checkInId.value}' }] -> ()
                    SET r.latitude = '${loc.latitude}'
                    SET r.longitude = '${loc.longitude}'
                    """.trimIndent()
                )
            }
        }
    }

    fun recordPeerToPeerScan(scanner: DeviceId, scanned: DeviceId, loc: Location?): ScanId? {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                            MATCH (d:Device) WHERE ID(d) = ${scanner.value}
                            MATCH (d2:Device) WHERE ID(d2) = ${scanned.value}
                            CREATE (d)-[r:SCAN{id: '${UUID.randomUUID()}', timestamp: TIMESTAMP(), latitude: '${loc?.latitude}', longitude: '${loc?.longitude}'}]->(d2)
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
     * @param testing if the organization owns sites that can do testing.
     * @param multiSite reporting if the organization has multiple sites.
     *
     * @return organization id.
     *
     * @throws exception if phone number is invalid.
     */
    fun createOrganization(organization: String, phone: String, email: String,
                           contactName: String, address: String, testing: Boolean,
                           multiSite: Boolean): OrganizationId? {
        val validNum = phoneUtil.isValidNumber(phoneUtil.parseAndKeepRawInput(phone, "US"))

        if (!validNum) {
            throw InvalidPhoneNumberException("Invalid phone number")
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
                                    hasTestingFacilities: '${testing}',
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
    fun setMultiSite(id: OrganizationId, state: Boolean) {
        driver.session().use {
            it.writeTransaction { txn ->
                txn.run(
                        """
                        MATCH (o:Organization { id: '${id.value}' })
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
     * @param testing whether the site can preform testing.
     * @param phone contact phone of site manager
     * @param email contact email of site manager
     * @param contactName contact name of site manager
     */
    fun createSite(id: OrganizationId, name: String, category: String,
                   subcategory: String, lat: Float, long: Float,
                   testing: Boolean, phone: String?, email: String?,
                   contactName: String?): SiteId? {
        val query = """
            MATCH (o:Organization { id: '${id.value}' })
            CREATE (s:Site {
                        id: '${UUID.randomUUID()}',
                        phone: '${phone}',
                        email: '${email}',
                        contactName: '${contactName}',
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
     *
     * @return list of all the sites.
     */
    fun getSites(id: OrganizationId): List<Pair<String, String>> {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        MATCH (o:Organization { id: '${id.value}' }) - [:OWNS] -> (s:Site)
                        RETURN s.id AS id, s.name AS name
                        """.trimIndent()
                ).list { x -> Pair(x["id"].asString(), x["name"].asString()) }
                return@writeTransaction result?.let { it }
            }
        }!!
    }

    /**
     * Creates a scannable for a site. A scannable is either QR Code or BT
     * receivers.
     *
     * @param oid organization id
     * @param sid site id
     * @param type type of scannable
     * @param singleUse if it is a single use scannable or not
     *
     * @return id of the scannable.
     */
    fun createScannable(oid: OrganizationId, sid: SiteId, type: String,
                        singleUse: Boolean): ScannableId? {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        MATCH (o:Organization { id: '${oid.value}' }) - [:OWNS] -> (s:Site { id: '${sid.value}' })
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

    /**
     * Creates a user node and links to device id.
     *
     * @param name name of the user.
     * @param phone phone of the user.
     * @param email email of the user.
     * @param id id of the device used to create it.
     *
     * @returns id of the user.
     */
    fun createUser(name: String?, phone: String?, email: String?, id: DeviceId): UserId? {
        if (phone?.let { !phoneUtil.isValidNumber(phoneUtil.parseAndKeepRawInput(phone, "US")) } ?: true) {
            throw InvalidPhoneNumberException("Invalid phone number")
        }

        // Can cause issues as it throws an exception when device id was used
        // to create another device
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                            MATCH (d:Device { id: '${id.value}' })
                            WHERE NOT () - [:OWNS] -> (d)
                            CREATE (u: User {
                                        id: '${UUID.randomUUID()}',
                                        name: '${name}',
                                        phone: '${phone}',
                                        email: '${email}',
                                        deleted: 'false'
                            }) - [r:OWNS] -> (d)
                            RETURN u.id as id
                        """.trimIndent()
                ).single()["id"].asString()
                return@writeTransaction result?.let { UserId(it) }
            }
        }
    }

    /**
     * "Deletes" the user
     *
     * @param id id of the user to delete
     */
    fun deleteUser(id: UserId) {
        driver.session().use {
            it.writeTransaction { txn ->
                txn.run(
                        """
                        MATCH (u:User { id: '${id.value}' })
                        SET u.deleted = 'true'
                        """
                )
            }
        }
    }

    /**
     * Gets the user
     *
     * @param id the id of the user
     *
     * @return User struct
     */
    fun getUser(id: UserId): User {
        return driver.session().use {
            it.writeTransaction { txn ->
                val result = txn.run(
                        """
                        MATCH (u:User { id: '${id.value}', deleted: 'false' })
                        RETURN u.name AS name, u.phone AS phone, u.email AS email, u.id AS id
                        """.trimIndent()
                ).single()
                return@writeTransaction result?.let { User(it["name"].asString(), it["phone"].asString(), it["email"].asString(), it["id"].asString()) }
            }
        }!!
    }
}

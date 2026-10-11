package com.example.cityflow.domain

import com.example.cityflow.data.model.Report
import com.example.cityflow.data.model.ReportCategory
import com.example.cityflow.data.model.ReportStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests για τις νέες λειτουργίες:
 * 1. Αιτιολογία Απόρριψης (Rejection Justification / Reason)
 * 2. Ανακοίνωση Επίλυσης (Resolution Announcement)
 */
class ReportRejectionAndResolutionTest {

    @Test
    fun testReportModel_DefaultRejectionAndResolutionFields() {
        val report = Report(
            id = "rep_test_1",
            category = ReportCategory.POTHOLE.name,
            description = "Μεγάλη λακκούβα στο δρόμο"
        )

        assertEquals("", report.rejectionReason)
        assertEquals("", report.resolutionAnnouncement)
        assertNull(report.resolvedAt)
        assertEquals(ReportStatus.SUBMITTED, report.reportStatus)
    }

    @Test
    fun testReportModel_RejectionReasonStored() {
        val reason = "Ψευδής αναφορά - Το σημείο ελέγχθηκε και δεν υπάρχει εμπόδιο."
        val rejectedReport = Report(
            id = "rep_test_2",
            status = ReportStatus.REJECTED.name,
            rejectionReason = reason
        )

        assertEquals(ReportStatus.REJECTED, rejectedReport.reportStatus)
        assertEquals(reason, rejectedReport.rejectionReason)
        assertFalse(rejectedReport.isVerified)
    }

    @Test
    fun testReportModel_ResolutionAnnouncementStored() {
        val announcement = "Η βλάβη αποκαταστάθηκε επιτυχώς από τα συνεργεία του Δήμου."
        val now = System.currentTimeMillis()
        val resolvedReport = Report(
            id = "rep_test_3",
            status = ReportStatus.RESOLVED.name,
            resolutionAnnouncement = announcement,
            resolvedAt = now
        )

        assertEquals(ReportStatus.RESOLVED, resolvedReport.reportStatus)
        assertEquals(announcement, resolvedReport.resolutionAnnouncement)
        assertEquals(now, resolvedReport.resolvedAt)
        assertTrue(resolvedReport.isVerified)
    }
}

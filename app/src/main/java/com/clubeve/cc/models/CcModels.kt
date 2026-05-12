package com.clubeve.cc.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── CC Event (richer than the PR Event model) ─────────────────────────────────
@Serializable
data class CcEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("club_name") val clubName: String = "",
    val location: String? = null,
    @SerialName("event_date") val eventDate: String = "",
    @SerialName("registration_deadline") val registrationDeadline: String? = null,
    @SerialName("max_capacity") val maxCapacity: Int? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("approval_status") val approvalStatus: String = "draft",
    @SerialName("feedback_open") val feedbackOpen: Boolean = false,
    @SerialName("feedback_config") val feedbackConfig: List<FeedbackQuestion>? = null,
    @SerialName("targeted_department") val targetedDepartment: String? = null,
    @SerialName("rejection_data") val rejectionData: List<RejectionRemark>? = null,
    @SerialName("status") val status: String? = null
)

@Serializable
data class FeedbackQuestion(
    val type: String = "text",   // "text" | "rating" | "multiple_choice"
    val label: String = "",
    val options: List<String>? = null
)

@Serializable
data class RejectionRemark(
    val field: String,
    val reason: String
)

// ── Report ────────────────────────────────────────────────────────────────────
@Serializable
data class CcReport(
    val id: String? = null,
    @SerialName("event_id") val eventId: String,
    val content: ReportContent,
    val status: String = "draft",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ReportContent(
    val summary: String = "",
    val outcomes: List<String> = emptyList(),
    val photos: List<String> = emptyList()
)

// ── Pipeline stats ────────────────────────────────────────────────────────────
data class PipelineStats(
    val drafts: Int = 0,
    val pending: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0
)

// ── Approval status helpers ───────────────────────────────────────────────────
object ApprovalStatus {
    const val DRAFT            = "draft"
    const val PENDING_PR       = "pending_pr"
    const val PENDING_TEACHER  = "pending_teacher"
    const val PENDING_HOD      = "pending_hod"
    const val APPROVED         = "approved"
    const val REJECTED         = "rejected"

    val PENDING_STATUSES = listOf(PENDING_PR, PENDING_TEACHER, PENDING_HOD)

    fun displayLabel(status: String): String = when (status) {
        DRAFT           -> "Draft"
        PENDING_PR      -> "Review: PR"
        PENDING_TEACHER -> "Review: Teacher"
        PENDING_HOD     -> "Review: HOD"
        APPROVED        -> "Approved"
        REJECTED        -> "Rejected"
        else            -> status.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    /** 0-based step index for the status stepper (0..4) */
    fun stepIndex(status: String): Int = when (status) {
        DRAFT           -> 0
        PENDING_PR      -> 1
        PENDING_TEACHER -> 2
        PENDING_HOD     -> 3
        APPROVED        -> 4
        REJECTED        -> -1   // special — shown separately
        else            -> 0
    }
}

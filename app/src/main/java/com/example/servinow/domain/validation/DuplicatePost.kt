package com.example.servinow.domain.validation

import com.example.servinow.domain.model.WorkPost
import com.example.servinow.domain.model.PostStatus
import java.text.Normalizer
import java.util.Locale

private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Only identical normalized content by the same author in an active request is a duplicate. */
fun isDuplicatePost(candidate: WorkPost, existing: WorkPost): Boolean =
    candidate.id != existing.id && candidate.author == existing.author &&
    existing.status in setOf(PostStatus.PENDING, PostStatus.VERIFIED, PostStatus.IN_PROGRESS, PostStatus.REJECTED) &&
    normalized(candidate.title) == normalized(existing.title) &&
    normalized(candidate.description) == normalized(existing.description) &&
    normalized(candidate.category) == normalized(existing.category) &&
    normalized(candidate.city) == normalized(existing.city)

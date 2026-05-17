package com.openlab.qualitos.industry.domain.model;

/** Lightweight reference to a normative standard (full model lives in Standards Hub / P4). */
public record Norm(String id, String fullName, String publisher) {}

package com.openlab.qualitos.quality.complaints;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComplaintResponseRepository extends JpaRepository<ComplaintResponse, UUID> {

    Page<ComplaintResponse> findByComplaintIdOrderBySentAtAsc(UUID complaintId, Pageable pageable);

    long countByComplaintId(UUID complaintId);

    long countByComplaintIdAndInternalNote(UUID complaintId, boolean internalNote);

    void deleteByComplaintId(UUID complaintId);
}

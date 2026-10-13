package com.example.bpmn.delegate;

import com.example.bpmn.model.LeaveRequest;
import com.example.bpmn.repository.LeaveRequestRepository;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Service Task Delegate: Notifikasi ke Mahasiswa (Notify Student).
 * Mengirimkan notifikasi (email/SMS/sistem) kepada mahasiswa mengenai
 * hasil akhir pengajuan cuti (disetujui atau ditolak beserta catatannya).
 */
@Component("notifyStudentDelegate")
public class NotifyStudentDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(NotifyStudentDelegate.class);

    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public NotifyStudentDelegate(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("=== [BPMN SERVICE TASK] Mengirim Notifikasi ke Mahasiswa ===");
        log.info("Process Instance ID: {}", execution.getProcessInstanceId());

        Long requestId = null;
        Object reqIdObj = execution.getVariable("requestId");
        if (reqIdObj instanceof Number) {
            requestId = ((Number) reqIdObj).longValue();
        } else if (reqIdObj instanceof String) {
            requestId = Long.parseLong((String) reqIdObj);
        }

        if (requestId == null) {
            log.error("ID Permohonan tidak ditemukan di variabel proses!");
            return;
        }

        Optional<LeaveRequest> optRequest = leaveRequestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            log.error("Permohonan cuti ID {} tidak ditemukan di database!", requestId);
            return;
        }

        LeaveRequest request = optRequest.get();
        Boolean advisorApproved = (Boolean) execution.getVariable("advisorApproved");
        Boolean kaprodiApproved = (Boolean) execution.getVariable("kaprodiApproved");

        String studentName = request.getStudentName();
        String nim = request.getNim();

        // Tentukan pesan notifikasi berdasarkan alur keputusan
        if (Boolean.FALSE.equals(advisorApproved)) {
            // Ditolak oleh Dosen Pembimbing
            String comment = (String) execution.getVariable("advisorComment");
            if (comment == null) comment = request.getAdvisorComment();
            request.setStatus("DITOLAK_DOSEN");
            if (comment != null) request.setAdvisorComment(comment);

            log.warn("================================================================================");
            log.warn("[NOTIFIKASI MAHASISWA] Yth. {} (NIM: {}):", studentName, nim);
            log.warn("Pengajuan cuti Anda DITOLAK oleh Dosen Pembimbing.");
            log.warn("Catatan Dosen: {}", (comment != null ? comment : "Tidak ada catatan"));
            log.warn("================================================================================");

        } else if (Boolean.FALSE.equals(kaprodiApproved)) {
            // Ditolak oleh Ketua Program Studi
            String comment = (String) execution.getVariable("kaprodiComment");
            if (comment == null) comment = request.getKaprodiComment();
            request.setStatus("DITOLAK_KAPRODI");
            if (comment != null) request.setKaprodiComment(comment);

            log.warn("================================================================================");
            log.warn("[NOTIFIKASI MAHASISWA] Yth. {} (NIM: {}):", studentName, nim);
            log.warn("Pengajuan cuti Anda DITOLAK oleh Ketua Program Studi.");
            log.warn("Catatan Kaprodi: {}", (comment != null ? comment : "Tidak ada catatan"));
            log.warn("================================================================================");

        } else if (Boolean.TRUE.equals(kaprodiApproved)) {
            // Disetujui Penuh oleh Ketua Program Studi
            String comment = (String) execution.getVariable("kaprodiComment");
            if (comment == null) comment = request.getKaprodiComment();
            request.setStatus("DISETUJUI");
            if (comment != null) request.setKaprodiComment(comment);

            log.info("================================================================================");
            log.info("[NOTIFIKASI MAHASISWA] Yth. {} (NIM: {}):", studentName, nim);
            log.info("SELAMAT! Pengajuan cuti Anda telah DISETUJUI oleh Ketua Program Studi.");
            log.info("Periode Cuti: {} s/d {}", request.getStartDate(), request.getEndDate());
            log.info("Catatan Kaprodi: {}", (comment != null ? comment : "Permohonan disetujui tanpa catatan"));
            log.info("Surat izin cuti resmi dapat diambil di bagian akademik.");
            log.info("================================================================================");
        } else {
            log.info("[NOTIFIKASI MAHASISWA] Status pengajuan cuti Mahasiswa {} (NIM: {}) diperbarui menjadi: {}",
                    studentName, nim, request.getStatus());
        }

        leaveRequestRepository.save(request);
        log.info("=== [BPMN SERVICE TASK] Notifikasi berhasil dikirim. Status Akhir: {} ===", request.getStatus());
    }
}

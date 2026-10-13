package com.example.bpmn.delegate;

import com.example.bpmn.model.LeaveRequest;
import com.example.bpmn.repository.LeaveRequestRepository;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service Task Delegate: Validasi Permohonan Cuti Mahasiswa.
 * Memeriksa kelengkapan dan keabsahan data permohonan cuti (NIM, tanggal, alasan)
 * sebelum diteruskan ke Dosen Pembimbing.
 */
@Component("validateRequestDelegate")
public class ValidateRequestDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ValidateRequestDelegate.class);

    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public ValidateRequestDelegate(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("=== [BPMN SERVICE TASK] Memulai Validasi Permohonan Cuti ===");
        log.info("Process Instance ID: {}", execution.getProcessInstanceId());

        Long requestId = null;
        Object reqIdObj = execution.getVariable("requestId");
        if (reqIdObj instanceof Number) {
            requestId = ((Number) reqIdObj).longValue();
        } else if (reqIdObj instanceof String) {
            requestId = Long.parseLong((String) reqIdObj);
        }

        if (requestId == null) {
            log.error("ID Permohonan (requestId) tidak ditemukan di variabel proses!");
            execution.setVariable("isValid", false);
            return;
        }

        Optional<LeaveRequest> optRequest = leaveRequestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            log.error("Permohonan cuti dengan ID {} tidak ditemukan di database!", requestId);
            execution.setVariable("isValid", false);
            return;
        }

        LeaveRequest request = optRequest.get();
        boolean valid = true;
        StringBuilder validationMsg = new StringBuilder();

        // 1. Validasi Nama Mahasiswa
        if (request.getStudentName() == null || request.getStudentName().trim().isEmpty()) {
            valid = false;
            validationMsg.append("Nama mahasiswa wajib diisi; ");
        }

        // 2. Validasi NIM
        if (request.getNim() == null || request.getNim().trim().length() < 5) {
            valid = false;
            validationMsg.append("NIM tidak valid (minimal 5 karakter); ");
        }

        // 3. Validasi Alasan Cuti
        if (request.getReason() == null || request.getReason().trim().length() < 5) {
            valid = false;
            validationMsg.append("Alasan cuti harus jelas (minimal 5 karakter); ");
        }

        // 4. Validasi Tanggal Cuti
        if (request.getStartDate() == null || request.getEndDate() == null) {
            valid = false;
            validationMsg.append("Tanggal mulai dan akhir cuti wajib diisi; ");
        } else if (request.getStartDate().isAfter(request.getEndDate())) {
            valid = false;
            validationMsg.append("Tanggal mulai cuti tidak boleh setelah tanggal selesai; ");
        }

        if (valid) {
            log.info("Validasi BERHASIL untuk Mahasiswa: {} (NIM: {}), ID Pengajuan: {}",
                    request.getStudentName(), request.getNim(), request.getId());
            execution.setVariable("isValid", true);
            request.setStatus("MENUNGGU_PERSETUJUAN_DOSEN");
        } else {
            log.warn("Validasi GAGAL untuk ID Pengajuan {}: {}", request.getId(), validationMsg);
            execution.setVariable("isValid", false);
            request.setStatus("VALIDASI_GAGAL");
        }

        leaveRequestRepository.save(request);
        log.info("=== [BPMN SERVICE TASK] Selesai Validasi. Status: {} ===", request.getStatus());
    }
}

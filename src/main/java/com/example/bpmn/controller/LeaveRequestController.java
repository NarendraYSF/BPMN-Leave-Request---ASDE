package com.example.bpmn.controller;

import com.example.bpmn.dto.TaskDto;
import com.example.bpmn.model.LeaveRequest;
import com.example.bpmn.repository.LeaveRequestRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Controller utama untuk mengelola alur kerja Pengajuan Cuti Mahasiswa.
 * Mengintegrasikan Spring MVC, Spring Data JPA, dan Camunda BPM Engine API.
 */
@Controller
public class LeaveRequestController {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestController.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public LeaveRequestController(RuntimeService runtimeService,
                                  TaskService taskService,
                                  LeaveRequestRepository leaveRequestRepository) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * Endpoint Pengujian Hello World untuk verifikasi instalasi & konfigurasi
     * Spring Boot + Jakarta/J2EE + Camunda BPMN.
     */
    @GetMapping("/hello")
    @ResponseBody
    public String helloWorld() {
        log.info("Endpoint Hello World diakses.");
        return "Hello World - BPMN Leave Request System is running!";
    }

    /**
     * Halaman Utama: Formulir Pengajuan Cuti Mahasiswa.
     */
    @GetMapping("/")
    public String index(Model model) {
        if (!model.containsAttribute("leaveRequest")) {
            model.addAttribute("leaveRequest", new LeaveRequest());
        }
        return "index";
    }

    /**
     * Memproses pengajuan cuti baru dari Mahasiswa.
     * Memulai instance proses alur kerja BPMN di Camunda Engine.
     */
    @PostMapping("/submit")
    public String submitLeaveRequest(@ModelAttribute LeaveRequest leaveRequest,
                                     RedirectAttributes redirectAttributes) {
        try {
            log.info("Menerima pengajuan cuti untuk: {} (NIM: {})",
                    leaveRequest.getStudentName(), leaveRequest.getNim());

            // 1. Simpan entitas awal ke database H2
            leaveRequest.setStatus("DIAJUKAN");
            LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

            // 2. Siapkan variabel proses BPMN
            Map<String, Object> variables = new HashMap<>();
            variables.put("requestId", savedRequest.getId());
            variables.put("studentName", savedRequest.getStudentName());
            variables.put("nim", savedRequest.getNim());
            variables.put("reason", savedRequest.getReason());
            variables.put("startDate", savedRequest.getStartDate().toString());
            variables.put("endDate", savedRequest.getEndDate().toString());

            // 3. Jalankan instance proses BPMN Camunda
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "leave-request-process",
                    String.valueOf(savedRequest.getId()),
                    variables
            );

            log.info("Instance proses BPMN berhasil dibuat dengan ID: {}", processInstance.getId());
            savedRequest.setProcessInstanceId(processInstance.getId());
            leaveRequestRepository.save(savedRequest);

            // 4. Selesaikan user task pertama (submitRequestTask) agar alur berlanjut ke validasi & review
            List<Task> initialTasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .taskDefinitionKey("submitRequestTask")
                    .list();

            for (Task t : initialTasks) {
                taskService.complete(t.getId(), variables);
                log.info("User task inisial '{}' (ID: {}) diselesaikan otomatis.", t.getName(), t.getId());
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Permohonan cuti atas nama " + savedRequest.getStudentName() +
                            " (NIM: " + savedRequest.getNim() + ") berhasil diajukan dan masuk ke dalam workflow BPMN!");

        } catch (Exception e) {
            log.error("Gagal memproses permohonan cuti: ", e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Gagal mengajukan permohonan: " + e.getMessage());
        }

        return "redirect:/requests";
    }

    /**
     * Halaman Daftar Tugas (Task Inbox) untuk Dosen Pembimbing dan Ketua Prodi.
     */
    @GetMapping("/tasks")
    public String showTasks(Model model) {
        List<Task> camundaTasks = taskService.createTaskQuery()
                .orderByTaskCreateTime().desc()
                .list();

        List<TaskDto> taskDtoList = new ArrayList<>();
        for (Task task : camundaTasks) {
            TaskDto dto = new TaskDto(
                    task.getId(),
                    task.getName(),
                    task.getTaskDefinitionKey(),
                    task.getAssignee(),
                    task.getCreateTime(),
                    task.getProcessInstanceId()
            );

            // Ambil variabel konteks permohonan cuti
            Object reqIdObj = taskService.getVariable(task.getId(), "requestId");
            if (reqIdObj != null) {
                Long reqId = (reqIdObj instanceof Number) ? ((Number) reqIdObj).longValue() : Long.parseLong(reqIdObj.toString());
                dto.setRequestId(reqId);
                leaveRequestRepository.findById(reqId).ifPresent(req -> {
                    dto.setStudentName(req.getStudentName());
                    dto.setNim(req.getNim());
                    dto.setReason(req.getReason());
                    dto.setStartDate(req.getStartDate() != null ? req.getStartDate().toString() : "");
                    dto.setEndDate(req.getEndDate() != null ? req.getEndDate().toString() : "");
                    dto.setStatus(req.getStatus());
                });
            }
            taskDtoList.add(dto);
        }

        model.addAttribute("tasks", taskDtoList);
        return "tasks";
    }

    /**
     * Memproses keputusan persetujuan/penolakan atas suatu User Task BPMN.
     */
    @PostMapping("/tasks/{taskId}/complete")
    public String completeTask(@PathVariable String taskId,
                               @RequestParam boolean approved,
                               @RequestParam(required = false, defaultValue = "") String comment,
                               RedirectAttributes redirectAttributes) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tugas tidak ditemukan!");
                return "redirect:/tasks";
            }

            log.info("Menyelesaikan Task: '{}' (ID: {}), Keputusan: {}, Catatan: {}",
                    task.getName(), taskId, approved ? "DISETUJUI" : "DITOLAK", comment);

            Object reqIdObj = taskService.getVariable(taskId, "requestId");
            Long requestId = null;
            if (reqIdObj != null) {
                requestId = (reqIdObj instanceof Number) ? ((Number) reqIdObj).longValue() : Long.parseLong(reqIdObj.toString());
            }

            Map<String, Object> taskVariables = new HashMap<>();

            if ("advisorReviewTask".equals(task.getTaskDefinitionKey())) {
                taskVariables.put("advisorApproved", approved);
                taskVariables.put("advisorComment", comment);

                if (requestId != null) {
                    leaveRequestRepository.findById(requestId).ifPresent(req -> {
                        req.setAdvisorComment(comment);
                        if (!approved) {
                            req.setStatus("DITOLAK_DOSEN");
                        } else {
                            req.setStatus("MENUNGGU_PERSETUJUAN_KAPRODI");
                        }
                        leaveRequestRepository.save(req);
                    });
                }

            } else if ("kaprodiReviewTask".equals(task.getTaskDefinitionKey())) {
                taskVariables.put("kaprodiApproved", approved);
                taskVariables.put("kaprodiComment", comment);

                if (requestId != null) {
                    leaveRequestRepository.findById(requestId).ifPresent(req -> {
                        req.setKaprodiComment(comment);
                        if (!approved) {
                            req.setStatus("DITOLAK_KAPRODI");
                        } else {
                            req.setStatus("DISETUJUI");
                        }
                        leaveRequestRepository.save(req);
                    });
                }
            }

            // Selesaikan User Task di Camunda Engine
            taskService.complete(taskId, taskVariables);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Tugas '" + task.getName() + "' berhasil diproses dengan keputusan: " +
                            (approved ? "DISETUJUI" : "DITOLAK"));

        } catch (Exception e) {
            log.error("Gagal menyelesaikan task {}: ", taskId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Gagal memproses tugas: " + e.getMessage());
        }

        return "redirect:/tasks";
    }

    /**
     * Halaman Monitoring: Menampilkan seluruh riwayat pengajuan cuti beserta statusnya.
     */
    @GetMapping("/requests")
    public String showAllRequests(Model model) {
        List<LeaveRequest> requests = leaveRequestRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("requests", requests);
        return "requests";
    }

    // ==========================================================================
    // REST API Endpoints (untuk integrasi & pengujian terotomasi)
    // ==========================================================================

    @GetMapping("/api/requests")
    @ResponseBody
    public ResponseEntity<List<LeaveRequest>> getAllRequestsApi() {
        return ResponseEntity.ok(leaveRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/api/tasks")
    @ResponseBody
    public ResponseEntity<List<TaskDto>> getTasksApi() {
        List<Task> camundaTasks = taskService.createTaskQuery().list();
        List<TaskDto> dtos = new ArrayList<>();
        for (Task t : camundaTasks) {
            TaskDto dto = new TaskDto(t.getId(), t.getName(), t.getTaskDefinitionKey(), t.getAssignee(), t.getCreateTime(), t.getProcessInstanceId());
            dtos.add(dto);
        }
        return ResponseEntity.ok(dtos);
    }
}

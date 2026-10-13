package com.example.bpmn;

import com.example.bpmn.model.LeaveRequest;
import com.example.bpmn.repository.LeaveRequestRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BpmnLeaveRequestApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    @DisplayName("Uji 1: Verifikasi Endpoint Hello World untuk Pengujian Instalasi & Konfigurasi")
    void testHelloWorldEndpoint() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello World - BPMN Leave Request System is running!"));
    }

    @Test
    @DisplayName("Uji 2: Verifikasi Alur Lengkap BPMN - Pengajuan Cuti Disetujui Penuh")
    void testFullLeaveRequestWorkflowApproval() throws Exception {
        // 1. Submit Permohonan Cuti Mahasiswa
        mockMvc.perform(post("/submit")
                        .param("studentName", "Narendra Yusuf Khaerudin")
                        .param("nim", "12409011030064")
                        .param("startDate", LocalDate.now().plusDays(7).toString())
                        .param("endDate", LocalDate.now().plusDays(14).toString())
                        .param("reason", "Keperluan keluarga di luar kota"))
                .andExpect(status().is3xxRedirection());

        // Cari entitas yang baru dibuat
        List<LeaveRequest> requests = leaveRequestRepository.findByNimOrderByCreatedAtDesc("12409011030064");
        assertThat(requests).isNotEmpty();
        LeaveRequest request = requests.get(0);
        assertThat(request.getStatus()).isEqualTo("MENUNGGU_PERSETUJUAN_DOSEN");

        // 2. Verifikasi User Task: Review Dosen Pembimbing
        List<Task> advisorTasks = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .taskDefinitionKey("advisorReviewTask")
                .list();
        assertThat(advisorTasks).hasSize(1);
        Task advisorTask = advisorTasks.get(0);

        // Selesaikan task dosen dengan persetujuan (APPROVED)
        mockMvc.perform(post("/tasks/" + advisorTask.getId() + "/complete")
                        .param("approved", "true")
                        .param("comment", "Disetujui, harap menjaga komunikasi"))
                .andExpect(status().is3xxRedirection());

        // 3. Verifikasi User Task: Review Ketua Program Studi
        List<Task> kaprodiTasks = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .taskDefinitionKey("kaprodiReviewTask")
                .list();
        assertThat(kaprodiTasks).hasSize(1);
        Task kaprodiTask = kaprodiTasks.get(0);

        // Selesaikan task kaprodi dengan persetujuan (APPROVED)
        mockMvc.perform(post("/tasks/" + kaprodiTask.getId() + "/complete")
                        .param("approved", "true")
                        .param("comment", "Disetujui resmi oleh Ketua Prodi"))
                .andExpect(status().is3xxRedirection());

        // 4. Verifikasi status akhir di database dan proses selesai
        LeaveRequest finalRequest = leaveRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(finalRequest.getStatus()).isEqualTo("DISETUJUI");
        assertThat(finalRequest.getAdvisorComment()).isEqualTo("Disetujui, harap menjaga komunikasi");
        assertThat(finalRequest.getKaprodiComment()).isEqualTo("Disetujui resmi oleh Ketua Prodi");

        // Verifikasi tidak ada active task lagi untuk instance ini
        long remainingTasks = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .count();
        assertThat(remainingTasks).isZero();
    }

    @Test
    @DisplayName("Uji 3: Verifikasi Alur BPMN - Penolakan oleh Dosen Pembimbing")
    void testLeaveRequestAdvisorRejection() throws Exception {
        // 1. Submit Permohonan Cuti
        mockMvc.perform(post("/submit")
                        .param("studentName", "Siti Nurhaliza")
                        .param("nim", "11220910000088")
                        .param("startDate", LocalDate.now().plusDays(2).toString())
                        .param("endDate", LocalDate.now().plusDays(30).toString())
                        .param("reason", "Liburan pribadi"))
                .andExpect(status().is3xxRedirection());

        List<LeaveRequest> requests = leaveRequestRepository.findByNimOrderByCreatedAtDesc("11220910000088");
        assertThat(requests).isNotEmpty();
        LeaveRequest request = requests.get(0);

        // Cari task dosen
        List<Task> advisorTasks = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .taskDefinitionKey("advisorReviewTask")
                .list();
        assertThat(advisorTasks).hasSize(1);
        Task advisorTask = advisorTasks.get(0);

        // Dosen menolak (REJECTED)
        mockMvc.perform(post("/tasks/" + advisorTask.getId() + "/complete")
                        .param("approved", "false")
                        .param("comment", "Alasan liburan pribadi tidak dapat disetujui untuk cuti akademik"))
                .andExpect(status().is3xxRedirection());

        // Verifikasi status akhir menjadi DITOLAK_DOSEN
        LeaveRequest finalRequest = leaveRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(finalRequest.getStatus()).isEqualTo("DITOLAK_DOSEN");
        assertThat(finalRequest.getAdvisorComment()).contains("Alasan liburan pribadi tidak dapat disetujui");

        // Verifikasi alur tidak pernah mencapai Ketua Prodi
        long kaprodiTaskCount = taskService.createTaskQuery()
                .processInstanceId(request.getProcessInstanceId())
                .taskDefinitionKey("kaprodiReviewTask")
                .count();
        assertThat(kaprodiTaskCount).isZero();
    }
}

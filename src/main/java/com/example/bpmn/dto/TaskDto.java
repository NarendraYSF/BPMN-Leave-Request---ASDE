package com.example.bpmn.dto;

import java.util.Date;

/**
 * Data Transfer Object (DTO) untuk menampilkan informasi User Task Camunda
 * beserta konteks data permohonan cuti mahasiswa terkait.
 */
public class TaskDto {

    private String taskId;
    private String taskName;
    private String taskDefinitionKey;
    private String assignee;
    private Date createTime;
    private String processInstanceId;

    // Konteks data pengajuan cuti
    private Long requestId;
    private String studentName;
    private String nim;
    private String reason;
    private String startDate;
    private String endDate;
    private String status;

    public TaskDto() {
    }

    public TaskDto(String taskId, String taskName, String taskDefinitionKey, String assignee, Date createTime, String processInstanceId) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskDefinitionKey = taskDefinitionKey;
        this.assignee = assignee;
        this.createTime = createTime;
        this.processInstanceId = processInstanceId;
    }

    // Getters and Setters

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

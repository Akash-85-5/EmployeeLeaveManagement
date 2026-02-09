package com.example.notificationservice.dto;

public class LeaveSummary {
    private int total;
    private int used;
    private int remaining;

    public LeaveSummary(int total,int used,int remaining){
        this.remaining = remaining;
        this.total = total;
        this.used = used;

    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getUsed() {
        return used;
    }

    public void setUsed(int used) {
        this.used = used;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }
}

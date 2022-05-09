package com.kubernetes.trafficmaker.schedule;

public class AlreadyScheduledException extends RuntimeException {

    public AlreadyScheduledException(String taskName) {
        super("Already scheduled task [%s]".formatted(taskName));
    }

}

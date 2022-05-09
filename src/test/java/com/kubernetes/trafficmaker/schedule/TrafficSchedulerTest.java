package com.kubernetes.trafficmaker.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrafficSchedulerTest {

    static final String INIT_TASK_NAME = "TASK_NAME";
    static final Runnable INIT_TASK = () -> {};
    static final Duration INIT_TASK_PERIOD = Duration.ofDays(1L);

    @Mock
    ThreadPoolTaskScheduler taskScheduler;

    @Mock
    ScheduledFuture scheduledFuture;

    TrafficScheduler trafficScheduler;

    @BeforeEach
    void setUp() {
        trafficScheduler = new TrafficScheduler(taskScheduler);

        // initialize - added one task (INIT_TASK_NAME)
        given(taskScheduler.scheduleAtFixedRate(INIT_TASK, INIT_TASK_PERIOD))
                .willReturn(scheduledFuture);
        trafficScheduler.addFixedRateSchedule(INIT_TASK_NAME, INIT_TASK, INIT_TASK_PERIOD);
    }

    @DisplayName("task가 등록되지 않았던 경우, addFixedRateSchedule 메서드는 task를 등록하고 true를 반환한다")
    @Test
    void return_true_addFixedRateSchedule() {
        // given
        var newTaskName = "TASK_NAME2";

        // when
        var isTaskScheduled = trafficScheduler.addFixedRateSchedule(newTaskName, INIT_TASK, INIT_TASK_PERIOD);

        // then
        assertThat(isTaskScheduled).isTrue();
        assertThat(trafficScheduler.isScheduled(newTaskName)).isTrue();
    }

    @DisplayName("중복된 이름의 Task가 들어오는 경우, addFixedRateSchedule는 false를 반환한다")
    @Test
    void return_false_addFixedRateSchedule() {
        // given
        var duplicatedTaskName = "TASK_NAME";

        // when
        var isTaskScheduled = trafficScheduler.addFixedRateSchedule(duplicatedTaskName, INIT_TASK, INIT_TASK_PERIOD);

        // then
        assertThat(isTaskScheduled).isFalse();
    }

    @DisplayName("등록되어져 있는 Task를 remove하면 해당 Task의 ScheduleFuture cancel 메서드가 호출된다")
    @Test
    void remove_call_cancel() {
        // given
        given(scheduledFuture.cancel(true)).willReturn(true);

        // when
        trafficScheduler.remove(INIT_TASK_NAME);

        // then
        verify(scheduledFuture).cancel(true);
    }

}
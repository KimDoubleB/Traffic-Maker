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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class TrafficSchedulerTest {

    static final String INIT_TASK_NAME = "TASK_NAME";
    static final Runnable INIT_TASK = () -> {
    };
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

    @DisplayName("등록되지 않은 task가 schedule 되면, task에 정상적으로 등록된다")
    @Test
    void addFixedRateScheduleTest_when_not_scheduled_task() {
        // given
        var newTaskName = "TASK_NAME2";

        // when
        trafficScheduler.addFixedRateSchedule(newTaskName, INIT_TASK, INIT_TASK_PERIOD);

        // then
        assertThat(trafficScheduler.isScheduledTask(newTaskName)).isTrue();
    }

    @DisplayName("task 업데이트 시, 같은 이름의 이미 존재하는 schedule task가 있으면 cancel하고 새로 등록한다")
    @Test
    void updateFixedRateScheduleTest_cancel_existed_schedule_task_and_register_new_schedule_task() {
        // given
        var alreadyScheduledTaskName = INIT_TASK_NAME;
        var newPeriod = Duration.ofMinutes(1L);
        Runnable newTask = () -> System.out.println("new TASK");
        var newScheduledFuture = mock(ScheduledFuture.class);

        given(scheduledFuture.cancel(true)).willReturn(true);
        given(taskScheduler.scheduleAtFixedRate(newTask, newPeriod))
                .willReturn(newScheduledFuture);

        // when
        trafficScheduler.updateFixedRateSchedule(alreadyScheduledTaskName, newTask, newPeriod);

        // then
        assertThat(trafficScheduler.isScheduledTask(alreadyScheduledTaskName)).isTrue();
        verify(scheduledFuture).cancel(true);
        verify(taskScheduler).scheduleAtFixedRate(newTask, newPeriod);
    }

    @DisplayName("task 업데이트 시, 같은 이름의 schedule task가 없으면 아무것도 하지 않는다")
    @Test
    void updateFixedRateScheduleTest_nothing_when_not_existed_same_task_name() {
        // given
        var newTaskName = "new task name";
        var newPeriod = Duration.ofMinutes(1L);
        Runnable newTask = () -> System.out.println("new TASK");

        // when
        trafficScheduler.updateFixedRateSchedule(newTaskName, newTask, newPeriod);

        // then
        assertThat(trafficScheduler.isScheduledTask(newTaskName)).isFalse();
        verify(scheduledFuture, never()).cancel(true);
        verify(taskScheduler, never()).scheduleAtFixedRate(newTask, newPeriod);
    }

    @DisplayName("task 제거 시, task name에 해당하는 schedule task가 있다면 종료하고 제거한다")
    @Test
    void removeSchedule_task_stop_and_remove_task() {
        // given
        var alreadyScheduledTaskName = INIT_TASK_NAME;
        given(scheduledFuture.cancel(true)).willReturn(true);

        // when
        trafficScheduler.removeSchedule(alreadyScheduledTaskName);

        // then
        assertThat(trafficScheduler.isScheduledTask(alreadyScheduledTaskName)).isFalse();
        verify(scheduledFuture).cancel(true);
    }

}
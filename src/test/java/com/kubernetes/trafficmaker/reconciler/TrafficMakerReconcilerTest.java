package com.kubernetes.trafficmaker.reconciler;

import com.kubernetes.trafficmaker.schedule.TrafficScheduler;
import com.kubernetes.trafficmaker.target.HttpTargetSpec;
import com.kubernetes.trafficmaker.target.TrafficTarget;
import com.kubernetes.trafficmaker.target.TrafficTargetSpec;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus;
import com.kubernetes.trafficmaker.target.TrafficTargetStatus.State;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.DefaultContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrafficMakerReconcilerTest {

    private static final DefaultContext<HasMetadata> DEFAULT_CONTEXT = new DefaultContext<>(null, null, null);
    private static final String DEFAULT_RESOURCE_NAME = "TrafficTarget";
    private static final String DEFAULT_RATE = "5m";

    @Mock
    TrafficScheduler trafficScheduler;

    TrafficMakerReconciler trafficMakerReconciler;

    @BeforeEach
    void setUp() {
        trafficMakerReconciler = new TrafficMakerReconciler(WebClient.create(), trafficScheduler);
    }

    @DisplayName("첫 Reconcile - 중복된 이름의 schedule task가 없는 경우, 성공한다")
    @Test
    void first_reconcile_not_exist_duplicated_schedule_task_success() {
        // given
        var trafficTarget = dummyTrafficTarget();
        var period = DurationStyle.detectAndParse(DEFAULT_RATE);

        // when
        var updateControl = trafficMakerReconciler.reconcile(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(trafficTarget.getStatus().state()).isEqualTo(State.SCHEDULING);
        verify(trafficScheduler).addFixedRateSchedule(eq(DEFAULT_RESOURCE_NAME), any(), eq(period));
    }

    @DisplayName("첫 Reconcile - 중복된 이름의 schedule task가 있는 경우, 실패한다")
    @Test
    void first_reconcile_exist_duplicated_schedule_task_failure() {
        // given
        var trafficTarget = dummyTrafficTarget();
        given(trafficScheduler.isScheduledTask(DEFAULT_RESOURCE_NAME)).willReturn(true);

        // when
        var updateControl = trafficMakerReconciler.reconcile(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(trafficTarget.getStatus().state()).isEqualTo(State.FAILURE);
    }

    @DisplayName("Failure state reconcile 시, 수정된 메니페스트가 정상적으로 등록이 가능하면 성공한다")
    @Test
    void success_valid_updated_manifest_when_failure_state() {
        // given
        var trafficTarget = dummyTrafficTarget();
        trafficTarget.setStatus(new TrafficTargetStatus(State.FAILURE));
        var period = DurationStyle.detectAndParse(DEFAULT_RATE);

        // when
        var updateControl = trafficMakerReconciler.reconcile(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(trafficTarget.getStatus().state()).isEqualTo(State.SCHEDULING);
        verify(trafficScheduler).addFixedRateSchedule(eq(DEFAULT_RESOURCE_NAME), any(), eq(period));
    }

    @DisplayName("Scheduling status인 경우 spec 정보가 업데이트 되었을 수도 있으니 schedule task를 업데이트한다")
    @Test
    void update_schedule_task_when_scheduling_status_exists_schedule_task() {
        // given
        var trafficTarget = dummyTrafficTarget();
        var period = DurationStyle.detectAndParse(DEFAULT_RATE);
        trafficTarget.setStatus(new TrafficTargetStatus(State.SCHEDULING));
        given(trafficScheduler.isScheduledTask(DEFAULT_RESOURCE_NAME)).willReturn(true);

        // when
        var updateControl = trafficMakerReconciler.reconcile(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(updateControl.isNoUpdate()).isTrue();
        assertThat(trafficTarget.getStatus().state()).isEqualTo(State.SCHEDULING);
        verify(trafficScheduler).updateFixedRateSchedule(eq(DEFAULT_RESOURCE_NAME), any(), eq(period));
    }

    @DisplayName("Scheduling status인 경우인데 schedule task가 존재하지 않는 경우, 현재 spec으로 schedule task를 추가한다")
    @Test
    void add_schedule_task_when_scheduling_status_not_exist_schedule_task() {
        // given
        var trafficTarget = dummyTrafficTarget();
        var period = DurationStyle.detectAndParse(DEFAULT_RATE);
        trafficTarget.setStatus(new TrafficTargetStatus(State.SCHEDULING));
        given(trafficScheduler.isScheduledTask(DEFAULT_RESOURCE_NAME)).willReturn(false);

        // when
        var updateControl = trafficMakerReconciler.reconcile(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(updateControl.isNoUpdate()).isTrue();
        assertThat(trafficTarget.getStatus().state()).isEqualTo(State.SCHEDULING);
        verify(trafficScheduler).addFixedRateSchedule(eq(DEFAULT_RESOURCE_NAME), any(), eq(period));
    }

    @DisplayName("Clean up 수행 시, finalizer가 정상제거된다")
    @Test
    void cleanUpTest() {
        // given
        var trafficTarget = dummyTrafficTarget();

        // when
        var deleteControl = trafficMakerReconciler.cleanup(trafficTarget, DEFAULT_CONTEXT);

        // then
        assertThat(deleteControl.isRemoveFinalizer()).isTrue();
        verify(trafficScheduler).removeSchedule(DEFAULT_RESOURCE_NAME);
    }

    TrafficTarget dummyTrafficTarget() {
        var trafficTarget = new TrafficTarget();
        trafficTarget.setMetadata(new ObjectMetaBuilder().withName(DEFAULT_RESOURCE_NAME).build());
        trafficTarget.setSpec(dummyTrafficTargetSpec());
        return trafficTarget;
    }

    TrafficTargetSpec dummyTrafficTargetSpec() {
        return new TrafficTargetSpec(dummyHttpTargetSpec(), DEFAULT_RATE);
    }

    HttpTargetSpec dummyHttpTargetSpec() {
        return new HttpTargetSpec(10, "uri", Map.of(), "body", "GET");
    }

}
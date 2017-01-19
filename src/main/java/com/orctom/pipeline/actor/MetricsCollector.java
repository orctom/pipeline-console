package com.orctom.pipeline.actor;

import akka.actor.ActorRef;
import com.google.common.base.CharMatcher;
import com.orctom.pipeline.annotation.Actor;
import com.orctom.pipeline.model.*;
import com.orctom.pipeline.precedure.AbstractMetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.Resource;

import static com.orctom.pipeline.Constants.MEMBER_EVENT_DOWN;
import static com.orctom.pipeline.Constants.MEMBER_EVENT_UP;

@Actor(role = "metrics-collector")
class MetricsCollector extends AbstractMetricsCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);
  
  @Resource
  private SimpMessagingTemplate template;

  @Override
  public void onMessage(PipelineMetrics metric) {
    LOGGER.debug(metric.toString());
    String metricType = metric.getKey();
    if ("routee".equals(metricType)) {
      send(new Message(
          metric.getTimestamp(),
          Type.CONNECTION,
          metric.getRole(),
          CharMatcher.anyOf("[]").trimFrom(metric.getGauge())
      ));

    } else if (MEMBER_EVENT_UP.equals(metricType)) {
      send(new Message(
          metric.getTimestamp(),
          Type.METER,
          metric.getApplicationName(),
          MEMBER_EVENT_UP,
          metric.getRole()
      ));

    } else if (MEMBER_EVENT_DOWN.equals(metricType)) {
      send(new Message(
          metric.getTimestamp(),
          Type.METER,
          metric.getApplicationName(),
          MEMBER_EVENT_DOWN,
          metric.getRole()
      ));

    } else {
      send(new Message(
          metric.getTimestamp(),
          Type.METER,
          metric.getRole(),
          metric.getKey(),
          metric.getValue() + " (" + metric.getRate() + "/s)"
      ));
    }
  }

  private void send(Message message) {
    template.convertAndSend("/topic/metrics", message);
  }

  @Override
  protected void memberAdded(ActorRef actor, MemberInfo memberInfo) {
    MetricsData.addApplication(memberInfo.getApplicationName(), memberInfo.getRoles());
  }

  @Override
  protected void memberRemoved(ActorRef actorRef) {
    super.memberRemoved(actorRef);
  }
}
package org.varamadon.autorefactor.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SerializationTest {

  @Test
  void actionPlanSerializationTest() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    String hash = "hash";
    ActionItem item1 = new AddCommentAction(5, "comment");
    ActionItem item2 = new RenameMethodAction(5, "old", "new");
    ActionPlan plan = new ActionPlan(hash, List.of(item1, item2));

    String serializedPlan = mapper.writeValueAsString(plan);
    assertEquals(plan, mapper.readValue(serializedPlan, ActionPlan.class));
  }

  @Test
  void actionItemSerializationTest() throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();

    ActionItem item = new RenameMethodAction(5, "old", "new");

    String serializedItem = mapper.writeValueAsString(item);
    assertEquals(item, mapper.readValue(serializedItem, ActionItem.class));
  }
}

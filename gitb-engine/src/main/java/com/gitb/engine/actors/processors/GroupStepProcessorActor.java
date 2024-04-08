package com.gitb.engine.actors.processors;

import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.tdl.Group;
import org.apache.pekko.actor.ActorRef;

/**
 * Created by serbay on 9/12/14.
 *
 * Group step executor actor
 */
public class GroupStepProcessorActor extends SequenceProcessorActor<Group> {
	public static final String NAME = "group-p";

	public GroupStepProcessorActor(Group step, TestCaseScope scope, String stepId) {
		super(step, scope, stepId);
	}

	public static ActorRef create(ActorContext context, Group step, TestCaseScope scope, String stepId) throws Exception {
		return create(GroupStepProcessorActor.class, context, step, scope, stepId);
	}
}

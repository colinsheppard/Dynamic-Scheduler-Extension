import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.nlogo.agent.ArrayAgentSet;
import org.nlogo.agent.World;
import org.nlogo.api.*;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.Workspace.OutputDestination;

public class DynamicSchedulerExtension
extends org.nlogo.api.DefaultClassManager {

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		return list;
	}

	// the WeakHashMap here may seem a bit odd, but it is apparently the easiest way to handle things
	// for explanation, see the comment in ArrayExtension.java in the Array extension.
	private static final java.util.WeakHashMap<LogoSchedule, Long> schedules = new java.util.WeakHashMap<LogoSchedule, Long>();
	private static long nextSchedule = 0;
	private static final java.util.WeakHashMap<LogoEvent, Long> events = new java.util.WeakHashMap<LogoEvent, Long>();
	private static long nextEvent = 0;

	public class LogoEvent
	// new NetLogo data types defined by extensions must implement
	// this interface
	implements org.nlogo.api.ExtensionObject {
		private final long id;
		public Double tick = null;
		public CommandTask task = null;
		public Agent agent = null;

		LogoEvent(Agent agent, CommandTask task, Double tick) {
			this.agent = agent;
			this.task = task;
			this.tick = tick;
			events.put(this, nextEvent);
			this.id = nextEvent;
			nextEvent++;
		}

		public void replaceData(Agent agent, CommandTask task, Double tick) {
			this.agent = agent;
			this.task = task;
			this.tick = tick;
		}

		/**
		 * This is a very shallow "equals", see recursivelyEqual()
		 * for deep equality.
		 */
		public boolean equals(Object obj) {
			return this == obj;
		}

		public String getExtensionName() {
			return "dynamic-scheduler";
		}

		public String getNLTypeName() {
			return "event";
		}

		public boolean recursivelyEqual(Object arg0) {
			// TODO Auto-generated method stub
			return equals(arg0);
		}

		public String dump(boolean arg0, boolean arg1, boolean arg2) {
			return tick + ((agent==null)?"":agent.toString()) + ((task==null)?"":task.toString());
		}
	}
	private static class LogoSchedule implements org.nlogo.api.ExtensionObject {
		private final long id;
		LogoEventComparator comparator = (new DynamicSchedulerExtension()).new LogoEventComparator();
		TreeSet<LogoEvent> schedule = new TreeSet<LogoEvent>(comparator);
		
		LogoSchedule() {
			schedules.put(this, nextSchedule);
			this.id = nextSchedule;
			nextSchedule++;
		}

		/**
		 * This is a very shallow "equals", see recursivelyEqual()
		 * for deep equality.
		 */
		public boolean equals(Object obj) {
			return this == obj;
		}

		public String dump(boolean readable, boolean exporting, boolean reference) {
			StringBuilder buf = new StringBuilder();
			if (exporting) {
				buf.append(id);
				if (!reference) {
					buf.append(":");
				}
			}
			if (!(reference && exporting)) {
				buf.append(" [ ");
				java.util.Iterator iter = schedule.iterator();
				while(iter.hasNext()){
					buf.append(((LogoEvent)iter.next()).dump(true, true, true));
					buf.append(" ");
				}
				buf.append("]");
			}
			return buf.toString();
		}

		public String getExtensionName() {
			return "dynamic-scheduler";
		}

		public String getNLTypeName() {
			return "schedule";
		}

		public boolean recursivelyEqual(Object arg0) {
			// TODO Auto-generated method stub
			return equals(arg0);
		}
	}

	/*
	 * The LogoEventComparator first compares based on tick (which is a Double) and then on id 
	 * so if there is a tie for tick, the event that was created first get's executed first allowing
	 * for a more intuitive execution.
	 */
	public class LogoEventComparator implements Comparator<LogoEvent> {
		public int compare(LogoEvent a, LogoEvent b) {
			if(a.tick < b.tick){
				return -1;
			}else if(a.tick > b.tick){
				return 1;
			}else if(a.id < b.id){
				return -1;
			}else if(a.id > b.id){
				return 1;
			}else{
				return 0;
			}
		}
	}

	public void clearAll() {
		schedules.clear();
		nextSchedule = 0;
	}

	///
	public void load(org.nlogo.api.PrimitiveManager primManager) {
		// dynamic-scheduler:size-of
		primManager.addPrimitive("size-of", new GetSize());
		// dynamic-scheduler:first
		primManager.addPrimitive("first", new First());
		// dynamic-scheduler:next
		primManager.addPrimitive("next", new Next());
		// dynamic-scheduler:add
		primManager.addPrimitive("add", new Add());
		// dynamic-scheduler:new
		primManager.addPrimitive("new", new NewLogoSchedule());
		// dynamic-scheduler:go
		primManager.addPrimitive("go", new PerformScheduledTasks());

		//		primManager.addPrimitive("copy", new Copy());
		//		primManager.addPrimitive("pretty-print-text", new PrettyPrintText());

	}

	///
	// Convenience method, to extract a schedule object from an Argument.
	private static LogoSchedule getScheduleFromArgument(Argument arg)
			throws ExtensionException, LogoException {
		Object obj = arg.get();
		if (!(obj instanceof LogoSchedule)) {
			throw new org.nlogo.api.ExtensionException("not a dynamic schedule: "
					+ org.nlogo.api.Dump.logoObject(obj));
		}
		return (LogoSchedule) obj;
	}

	public static class NewLogoSchedule extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{},
					Syntax.WildcardType());
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = new LogoSchedule();
			return sched;
		}
	}

	public static class First extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.WildcardType());
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			return sched.schedule.first();
		}
	}

	public static class Next extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.WildcardType());
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			Object toReturn = sched.schedule.first();
			sched.schedule.remove(toReturn);
			return toReturn;
		}
	}

	public static class GetSize extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
					Syntax.NumberType());
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);

			return sched.schedule.size();
		}
	}

	public static class Add extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.WildcardType(),
					Syntax.NumberType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			try {
				ExtensionContext extcontext = (ExtensionContext) context;
				extcontext.workspace().outputObject("scheduling agent: "+args[1].getAgent()+" task: "+args[2].getCommandTask().toString()+" tick: "+args[3].getDoubleValue(), 
						null, true, true,OutputDestination.OUTPUT_AREA);
			} catch (LogoException e) {
				throw new ExtensionException(e);
			}
			LogoEvent event = (new DynamicSchedulerExtension()).new LogoEvent(args[1].getAgent(),args[2].getCommandTask(),args[3].getDoubleValue());
			sched.schedule.add(event);
		}
	}

	public static class PerformScheduledTasks extends DefaultCommand {

		public Syntax getSyntax() {
			return Syntax.commandSyntax(new int[]{Syntax.WildcardType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, LogoException {
			ExtensionContext extcontext = (ExtensionContext) context;
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			Double ticks = extcontext.workspace().world().ticks();
			Boolean firstIteration = true;
			Object[] emptyArgs = new Object[0];
			// The outter while loop is here to catch any new events created dynamically during execution of the
			// already scheduled events
			LogoEvent event = sched.schedule.isEmpty() ? null : sched.schedule.first();
			while(event != null && event.tick < ticks + 1.0){
				org.nlogo.nvm.CommandTask nvmTask = (org.nlogo.nvm.CommandTask) event.task;
				org.nlogo.agent.Agent agentAgent = (org.nlogo.agent.Agent) event.agent;
				try {
					extcontext.workspace().outputObject("performing event-id: "+event.id+" with event tick:"+event.tick+" on ticks: "+ticks, 
							null, true, true,OutputDestination.OUTPUT_AREA);
				} catch (LogoException e) {
					throw new ExtensionException(e);
				}
				sched.schedule.remove(event);
				
				ArrayAgentSet agentSet = new ArrayAgentSet(agentAgent.getAgentClass(),1,false,(World) event.agent.world());
				agentSet.add(agentAgent);
				org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job, agentAgent,extcontext.nvmContext().ip,extcontext.nvmContext().activation);
				try {
					extcontext.workspace().outputObject("nvmContext.agent: "+nvmContext.agent,							
							null, true, true,OutputDestination.OUTPUT_AREA);
				} catch (LogoException e) {
					throw new ExtensionException(e);
				}
				 
				nvmTask.perform(nvmContext, emptyArgs);
				
				event = sched.schedule.isEmpty() ? null : sched.schedule.first();
			}
		}
	}



}


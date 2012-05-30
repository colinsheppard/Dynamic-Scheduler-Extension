import java.util.ArrayList;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.nlogo.agent.AgentSet.Iterator;
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
	private static boolean debug = true;

	public class LogoEvent
	// new NetLogo data types defined by extensions must implement
	// this interface
	implements org.nlogo.api.ExtensionObject {
		private final long id;
		public Double tick = null;
		public org.nlogo.nvm.CommandTask task = null;
		public org.nlogo.agent.AgentSet agents = null;

		LogoEvent(org.nlogo.agent.AgentSet agents, CommandTask task, Double tick) {
			this.agents = agents;
			this.task = (org.nlogo.nvm.CommandTask) task;
			this.tick = tick;
			events.put(this, nextEvent);
			this.id = nextEvent;
			nextEvent++;
		}

		public void replaceData(Agent agent, CommandTask task, Double tick) {
			this.agents = agents;
			this.task = (org.nlogo.nvm.CommandTask) task;
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
			return tick + ((agents==null)?"":agents.toString()) + ((task==null)?"":task.toString());
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
		primManager.addPrimitive("create", new NewLogoSchedule());
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
			throw new ExtensionException("not a dynamic schedule: "
					+ Dump.logoObject(obj));
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
			if(args.length<4)throw new ExtensionException("dynamic-scheduler:add must have 4 arguments: schedule agent tick task");
			if (!(args[0].get() instanceof LogoSchedule)) throw new ExtensionException("dynamic-scheduler:add expecting a schedule as the first argument");
			if (!(args[1].get() instanceof Agent) && !(args[1].get() instanceof AgentSet)) throw new ExtensionException("dynamic-scheduler:add expecting an agent or agent set as the second argument");
			if (!(args[2].get() instanceof CommandTask)) throw new ExtensionException("dynamic-scheduler:add expecting a command task as the third argument");
			if (!args[3].get().getClass().equals(Double.class)) throw new ExtensionException("dynamic-scheduler:add expecting a number as the fourth argument");
			
			org.nlogo.agent.AgentSet agentSet = null;
			if (args[1].get() instanceof org.nlogo.agent.Agent){
				org.nlogo.agent.Agent theAgent = (org.nlogo.agent.Agent)args[1].getAgent();
				agentSet = new ArrayAgentSet(theAgent.getAgentClass(),1,false,(World) theAgent.world());
				agentSet.add(theAgent);
			}else{
				agentSet = (org.nlogo.agent.AgentSet) args[1].getAgentSet();
			}
			if(debug)printToConsole(context,"scheduling agents: "+agentSet+" task: "+args[2].getCommandTask().toString()+" tick: "+args[3].getDoubleValue());
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			LogoEvent event = (new DynamicSchedulerExtension()).new LogoEvent(agentSet,args[2].getCommandTask(),args[3].getDoubleValue());
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
			Object[] emptyArgs = new Object[0]; // This extension is only for CommandTasks, so we know there aren't any args to pass in
			LogoEvent event = sched.schedule.isEmpty() ? null : sched.schedule.first();
			while(event != null && event.tick < ticks + 1.0){
				if(debug)printToConsole(context,"performing event-id: "+event.id+" for agent: "+event.agents+" with tick:"+event.tick+" on ticks: "+ticks);
				
				Iterator iter = event.agents.iterator();
				while(iter.hasNext()){
					org.nlogo.nvm.Context nvmContext = new org.nlogo.nvm.Context(extcontext.nvmContext().job,iter.next(),extcontext.nvmContext().ip,extcontext.nvmContext().activation);
					event.task.perform(nvmContext, emptyArgs);
				}
				
				sched.schedule.remove(event);
				event = sched.schedule.isEmpty() ? null : sched.schedule.first();
			}
		}
	}
	
	private static void printToConsole(Context context, String msg) throws ExtensionException{
		try {
			ExtensionContext extcontext = (ExtensionContext) context;
			extcontext.workspace().outputObject(msg,null, true, true,OutputDestination.OUTPUT_AREA);
		} catch (LogoException e) {
			throw new ExtensionException(e);
		}
	}

}


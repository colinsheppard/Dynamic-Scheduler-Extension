import java.util.ArrayList;

import org.nlogo.api.CompilerException;
import org.nlogo.api.LogoException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.Argument;
import org.nlogo.api.Syntax;
import org.nlogo.api.Context;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.DefaultCommand;

public class DynamicSchedulerExtension
extends org.nlogo.api.DefaultClassManager {

	public java.util.List<String> additionalJars() {
		java.util.List<String> list = new java.util.ArrayList<String>();
		return list;
	}

	// the WeakHashMap here may seem a bit odd, but it is apparently the easiest way to handle things
	// for explanation, see the comment in ArrayExtension.java in the Array extension.
	private static final java.util.WeakHashMap<LogoSchedule, Long> schedules = new java.util.WeakHashMap<LogoSchedule, Long>();
	private static long next = 0;

	private static class LogoSchedule
	// new NetLogo data types defined by extensions must implement
	// this interface
	implements org.nlogo.api.ExtensionObject {
		// NOTE: Because the Jama.Matrix does not support resizing/reshaping
		//       the underlying array, it turned out to be simpler to
		//       store the matrix in a member field, rather than have LogoMatrix
		//       be a subclass of Jama.Matrix.

		ArrayList<Double> schedule = null;
		private final long id;

		/**
		 * should be used only when doing importWorld, and
		 * we only have a reference to the Object, where the data
		 * will be defined later.
		 */
		LogoSchedule(long id) {
			schedule = null;
			this.id = id;
			schedules.put(this, id);
			next = StrictMath.max(next, id + 1);
		}

		LogoSchedule(ArrayList<Double> scheduleData) {
			schedule = scheduleData;
			schedules.put(this, next);
			this.id = next;
			next++;
		}

		public void replaceData(double[] dArray) {
			schedule = new ArrayList<Double>();
			for(double dub : dArray){
				schedule.add(dub);
			}
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
				for (int i = 0; i < schedule.size(); i++) {
						buf.append(org.nlogo.api.Dump.number(schedule.get(i)));
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
			// since this extension only defines one type, we don't
			// need to give it a name; "dynamic-scheduler:" is enough,
			// "dynamic-scheduler:schedule" would be redundant
			return "";
		}

		public boolean recursivelyEqual(Object arg0) {
			// TODO Auto-generated method stub
			return equals(arg0);
		}
	}

	public void clearAll() {
		schedules.clear();
		next = 0;
	}

	public StringBuilder exportWorld() {
		StringBuilder buffer = new StringBuilder();
		for (LogoSchedule sched : schedules.keySet()) {
			buffer.append(org.nlogo.api.Dump.csv().encode(org.nlogo.api.Dump.extensionObject(sched, true, true, false)) + "\n");
		}
		return buffer;
	}

	public void importWorld(java.util.List<String[]> lines, org.nlogo.api.ExtensionManager reader,
			org.nlogo.api.ImportErrorHandler handler) {
		for (String[] line : lines) {
			try {
				reader.readFromString(line[0]);
			} catch (CompilerException e) {
				handler.showError("Error importing dynamic schedule", e.getMessage(), "This schedule will be ignored");
			}
		}
	}

	public org.nlogo.api.ExtensionObject readExtensionObject(org.nlogo.api.ExtensionManager reader,
			String typeName, String value)
					throws CompilerException, ExtensionException {
		String[] s = value.split(":");
		long id = Long.parseLong(s[0]);
		LogoSchedule sched = getOrCreateScheduleFromId(id);
		if (s.length > 1) {
			LogoList nestedL = (LogoList) reader.readFromString(s[1]);
			double[] newData = convertNestedLogoListToArray(nestedL);
			sched.replaceData(newData);
		}
		return sched;
	}

	private static double[] convertNestedLogoListToArray(LogoList nestedLogoList) throws ExtensionException {
		int numRows = nestedLogoList.size();
		if (numRows == 0) {
			throw new ExtensionException("input list was empty");
		}
		int numElements = ((LogoList)nestedLogoList.get(0)).size();
		// find out the maximum column size of any of the rows,
		// in case we have a "ragged" right edge, where some rows
		// have more columns than others.
		if (numElements == 0) {
			throw new ExtensionException("input list is empty");
		}
		double[] array = new double[numElements];
		int i = 0;
		for (Object obj : ((LogoList)nestedLogoList.get(0))) {
			array[i++] = ((Number) obj).doubleValue();
		}
		// pad with zeros if we have a "ragged" right edge
		for (; i < numElements; i++) {
			array[i] = 0.0;
		}
		return array;
	}

	private static double[][] convertSimpleLogoListToArray(LogoList SimpleLogoList) throws ExtensionException {
		int numRows = 1;
		int numCols = SimpleLogoList.size();

		double[][] array = new double[numRows][numCols];
		int row = 0;
		for (int i = 0; i < numCols; i++) {
			array[row][i] = ((Number) SimpleLogoList.get(i)).doubleValue();
		}

		return array;
	}

	private static LogoList convertArrayToNestedLogoList(double[][] dArray) {
		LogoListBuilder lst = new LogoListBuilder();
		for (int i = 0; i < dArray.length; i++) {
			LogoListBuilder rowLst = new LogoListBuilder();
			for (int j = 0; j < dArray[i].length; j++) {
				rowLst.add(Double.valueOf(dArray[i][j]));
			}
			lst.add(rowLst.toLogoList());
		}
		return lst.toLogoList();
	}

	private static LogoList convertArrayToSimpleLogoList(double[][] dArray) {
		LogoListBuilder lst = new LogoListBuilder();
		for (int i = 0; i < dArray.length; i++) {
			for (int j = 0; j < dArray[i].length; j++) {
				lst.add(Double.valueOf(dArray[i][j]));
			}
		}
		return lst.toLogoList();
	}

	/**
	 * Used during import world, to recreate matrices with the
	 * correct id numbers, so all the references match up.
	 *
	 * @param id
	 * @return
	 */
	private LogoSchedule getOrCreateScheduleFromId(long id) {
		for (LogoSchedule sched : schedules.keySet()) {
			if (sched.id == id) {
				return sched;
			}
		}
		return new LogoSchedule(id);
	}

	///
	public void load(org.nlogo.api.PrimitiveManager primManager) {
		// matrix:get mat rowI colJ  =>  value at location I,J
		primManager.addPrimitive("size-of", new GetSize());
		// matrix:get mat rowI colJ  =>  value at location I,J
		primManager.addPrimitive("get", new Get());
		// matrix:set mat rowI colJ newValue
		primManager.addPrimitive("add", new Add());
		
		// matrix:copy mat => matrix object
//		primManager.addPrimitive("copy", new Copy());

		// matrix:pretty-print-text matrix => string containing formatted text
//		primManager.addPrimitive("pretty-print-text", new PrettyPrintText());

		//Note: The Jama library that we're using can do more than just the functionality
		//      that we've exposed here.  (e.g. LU, Cholesky, SV decomposition, determinants)
		//      Motivated persons could add more primitives to access these functions...

	}

	///
	// Convenience method, to extract a schedule object from an Argument.
	// It serves a similar purpose to args[x].getString(), or args[x].getList().
	private static LogoSchedule getScheduleFromArgument(Argument arg)
			throws ExtensionException, LogoException {
		Object obj = arg.get();
		if (!(obj instanceof LogoSchedule)) {
			throw new org.nlogo.api.ExtensionException("not a dynamic schedule: "
					+ org.nlogo.api.Dump.logoObject(obj));
		}
		return (LogoSchedule) obj;
	}

	public static class Get extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType(),
					Syntax.NumberType()},
					Syntax.WildcardType());
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			int index = args[1].getIntValue();

			if (index < 0 || index >= sched.schedule.size()) {
				throw new org.nlogo.api.ExtensionException(index + " is not avalid index for a schedule of size "
						+ sched.schedule.size());
			}
			return sched.schedule.get(index);
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
					Syntax.WildcardType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, LogoException {
			LogoSchedule sched = getScheduleFromArgument(args[0]);
			sched.schedule.add(args[3].getDoubleValue());
		}
	}

//	public static class Copy extends DefaultReporter {
//
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
//					Syntax.WildcardType());
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, LogoException {
//			return new LogoMatrix(getMatrixFromArgument(args[0]).matrix.copy());
//		}
//	}

//	public static class PrettyPrintText extends DefaultReporter {
//
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(new int[]{Syntax.WildcardType()},
//					Syntax.StringType());
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, LogoException {
//
//			double[][] dArray = getMatrixFromArgument(args[0]).matrix.getArray();
//			int maxLen[] = new int[dArray[0].length];
//			for (int j = 0; j < dArray[0].length; j++) {
//				maxLen[j] = 0;
//			}
//			for (int i = 0; i < dArray.length; i++) {
//				for (int j = 0; j < dArray[i].length; j++) {
//					int len = org.nlogo.api.Dump.number(dArray[i][j]).length();
//					if (len > maxLen[j]) {
//						maxLen[j] = len;
//					}
//				}
//			}
//
//			StringBuilder buf = new StringBuilder();
//			buf.append("[");
//			for (int i = 0; i < dArray.length; i++) {
//				if (i > 0) {
//					buf.append(" ");
//				}
//				buf.append("[");
//				for (int j = 0; j < dArray[i].length; j++) {
//					if (j != 0) {
//						buf.append(" ");
//					}
//					buf.append(" ");
//					buf.append(String.format("%" + maxLen[j] + "s", org.nlogo.api.Dump.number(dArray[i][j])));
//				}
//				buf.append(" ]");
//				if (i < dArray.length - 1) {
//					buf.append("\n");
//				}
//			}
//			buf.append("]");
//			return buf.toString();
//		}
//	}


}

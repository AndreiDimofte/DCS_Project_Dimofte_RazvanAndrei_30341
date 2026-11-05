package FuzzyUVS;

import java.util.ArrayList;

import Components.Activation;
import Components.Condition;
import Components.GuardMapping;
import Components.PetriNet;
import Components.PetriNetWindow;
import Components.PetriTransition;
import DataObjects.DataFuzzy;
import DataObjects.DataTransfer;
import DataOnly.FLRS;
import DataOnly.FV;
import DataOnly.Fuzzy;
import DataOnly.PlaceNameWithWeight;
import DataOnly.TransferOperation;
import Enumerations.FZ;
import Enumerations.LogicConnector;
import Enumerations.TransitionCondition;
import Enumerations.TransitionOperation;

public class Intersection {

	public static final String HOST = "localhost";
	public static final int INTERSECTION_PORT = 1080;
	public static final int[] LANE_PORTS = { 1081, 1082, 1083, 1084 };

	public static final float RED = -1F;
	public static final float GREEN = 0F;
	public static final float YELLOW = 1F;

	// 5 s light = 5 ticks of 1 s
	public static final int TICK_MS = 1000;
	public static final int LIGHT_TICKS = 5000 / TICK_MS;

	// lane tick and travel time
	public static final int LANE_TICK_MS = 500;
	public static final int LANE_TRAVEL_TICKS = 3;

	// compares by value (Fuzzy has no equals)
	public static class TrafficSignal extends Fuzzy {
		private static final long serialVersionUID = 1L;

		public TrafficSignal(float value) {
			super(value);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Fuzzy))
				return false;
			Fuzzy other = (Fuzzy) o;
			return Value != null && other.Value != null && Value.floatValue() == other.Value.floatValue();
		}

		@Override
		public int hashCode() {
			return Value == null ? 0 : Float.hashCode(Value);
		}
	}

	public static DataFuzzy signalPlace(String name, float value) {
		DataFuzzy p = new DataFuzzy();
		p.SetName(name);
		p.SetValue(new TrafficSignal(value));
		return p;
	}

	public static void main(String[] args) {
		PetriNet pn = createNet();

		System.out.println("Intersection started \n ------------------------------");
		pn.ShowLogInWindow = true;

		PetriNetWindow frame = new PetriNetWindow(false);
		frame.petriNet = pn;
		frame.setVisible(true);
	}

	public static PetriNet createNet() {
		PetriNet pn = new PetriNet();
		pn.PetriNetName = "Intersection";
		pn.NetworkPort = INTERSECTION_PORT;

		// FLRS tables
		// FLRS+
		FLRS flrsAdd = new FLRS(
				new FV(FZ.NL), new FV(FZ.NL), new FV(FZ.NL), new FV(FZ.NM), new FV(FZ.ZR),
				new FV(FZ.NL), new FV(FZ.NL), new FV(FZ.NM), new FV(FZ.ZR), new FV(FZ.PM),
				new FV(FZ.NL), new FV(FZ.NM), new FV(FZ.ZR), new FV(FZ.PM), new FV(FZ.PL),
				new FV(FZ.NM), new FV(FZ.ZR), new FV(FZ.PM), new FV(FZ.PL), new FV(FZ.PL),
				new FV(FZ.ZR), new FV(FZ.PM), new FV(FZ.PL), new FV(FZ.PL), new FV(FZ.PL));

		// FLRS Move
		FLRS flrsMove = new FLRS(new FV(FZ.NL), new FV(FZ.NM), new FV(FZ.ZR), new FV(FZ.PM), new FV(FZ.PL));

		flrsAdd.Print();
		flrsMove.Print();

		// Places
		for (int k = 1; k <= 4; k++)
			addFuzzyPlace(pn, "P_i" + k); // inputs from lane k

		addFuzzyPlace(pn, "P_i12");
		addFuzzyPlace(pn, "P_i34");
		addFuzzyPlace(pn, "P_i");

		for (int k = 1; k <= 4; k++)
			addFuzzyPlace(pn, "P_o" + k);

		// P_out1..4 to exit lanes
		for (int k = 1; k <= 4; k++) {
			DataTransfer pOut = new DataTransfer();
			pOut.SetName("P_out" + k);
			pOut.Value = new TransferOperation(HOST, String.valueOf(LANE_PORTS[k - 1]), "P_o" + k);
			pn.PlaceList.add(pOut);
		}

		// split ratios
		addConstantFuzzy(pn, "C_025", 0.25F);
		addConstantFuzzy(pn, "C_050", 0.50F);
		addConstantFuzzy(pn, "C_012", 0.12F);

		// Intersection
		addAdderTransition(pn, "t1", "P_i1", "P_i2", "P_i12", flrsAdd, flrsMove);
		addAdderTransition(pn, "t2", "P_i3", "P_i4", "P_i34", flrsAdd, flrsMove);
		addAdderTransition(pn, "t3", "P_i12", "P_i34", "P_i", flrsAdd, flrsMove);

		// t_s: split
		PetriTransition t_s = new PetriTransition(pn);
		t_s.TransitionName = "t_s";
		t_s.InputPlaceName.add("P_i");

		GuardMapping grdT_s = new GuardMapping();
		grdT_s.condition = new Condition(t_s, "P_i", TransitionCondition.NotNull);
		grdT_s.Activations.add(new Activation(product("P_i", "C_025"), TransitionOperation.Prod_Fuzzy, "P_o1", t_s));
		grdT_s.Activations.add(new Activation(product("P_i", "C_050"), TransitionOperation.Prod_Fuzzy, "P_o2", t_s));
		grdT_s.Activations.add(new Activation(product("P_i", "C_012"), TransitionOperation.Prod_Fuzzy, "P_o3", t_s));
		grdT_s.Activations.add(new Activation(product("P_i", "C_012"), TransitionOperation.Prod_Fuzzy, "P_o4", t_s));
		t_s.GuardMappingList.add(grdT_s);
		t_s.Delay = 0;
		pn.Transitions.add(t_s);

		// t_out1..4
		for (int k = 1; k <= 4; k++) {
			PetriTransition t_out = new PetriTransition(pn);
			t_out.TransitionName = "t_out" + k;
			t_out.InputPlaceName.add("P_o" + k);

			GuardMapping grdT_out = new GuardMapping();
			grdT_out.condition = new Condition(t_out, "P_o" + k, TransitionCondition.NotNull);
			grdT_out.Activations.add(new Activation(t_out, "P_o" + k, TransitionOperation.SendOverNetwork, "P_out" + k));
			t_out.GuardMappingList.add(grdT_out);
			t_out.Delay = 0;
			pn.Transitions.add(t_out);
		}

		// Traffic light controller
		pn.ConstantPlaceList.add(signalPlace("Red", RED));
		pn.ConstantPlaceList.add(signalPlace("Green", GREEN));
		pn.ConstantPlaceList.add(signalPlace("Yellow", YELLOW));

		// TL1..4 to lane P_TL
		for (int k = 1; k <= 4; k++) {
			DataTransfer tl = new DataTransfer();
			tl.SetName("TL" + k);
			tl.Value = new TransferOperation(HOST, String.valueOf(LANE_PORTS[k - 1]), "P_TL");
			pn.PlaceList.add(tl);
		}

		// phase places
		DataFuzzy pStart = new DataFuzzy();
		pStart.SetName("P_Start");
		pStart.SetValue(new Fuzzy(0F));
		pn.PlaceList.add(pStart);
		for (int ph = 1; ph <= 4; ph++)
			addFuzzyPlace(pn, "P_Ph" + ph);

		// 1,3 green - 2,4 red
		addPhaseTransition(pn, "t_init", "P_Start", "P_Ph1", 0, "Green", "Red");
		// 1,3 yellow - 2,4 red
		addPhaseTransition(pn, "t_ph1", "P_Ph1", "P_Ph2", LIGHT_TICKS, "Yellow", "Red");
		// 1,3 red - 2,4 green
		addPhaseTransition(pn, "t_ph2", "P_Ph2", "P_Ph3", LIGHT_TICKS, "Red", "Green");
		// 1,3 red - 2,4 yellow
		addPhaseTransition(pn, "t_ph3", "P_Ph3", "P_Ph4", LIGHT_TICKS, "Red", "Yellow");
		// 1,3 green - 2,4 red
		addPhaseTransition(pn, "t_ph4", "P_Ph4", "P_Ph1", LIGHT_TICKS, "Green", "Red");

		pn.Delay = TICK_MS;
		return pn;
	}

	// helpers

	private static void addFuzzyPlace(PetriNet pn, String name) {
		DataFuzzy p = new DataFuzzy();
		p.SetName(name);
		pn.PlaceList.add(p);
	}

	private static void addConstantFuzzy(PetriNet pn, String name, float value) {
		DataFuzzy c = new DataFuzzy();
		c.SetName(name);
		c.SetValue(new Fuzzy(value));
		pn.ConstantPlaceList.add(c);
	}

	private static ArrayList<PlaceNameWithWeight> product(String place, String constant) {
		ArrayList<PlaceNameWithWeight> input = new ArrayList<>();
		input.add(new PlaceNameWithWeight(place, 1F));
		input.add(new PlaceNameWithWeight(constant, 1F));
		return input;
	}

	// FLRS+, or FLRS Move when one input is empty
	private static void addAdderTransition(PetriNet pn, String name, String in1, String in2, String out,
			FLRS flrsAdd, FLRS flrsMove) {
		PetriTransition t = new PetriTransition(pn);
		t.TransitionName = name;
		t.InputPlaceName.add(in1);
		t.InputPlaceName.add(in2);

		ArrayList<String> output = new ArrayList<>();
		output.add(out);

		// both inputs
		Condition c11 = new Condition(t, in1, TransitionCondition.NotNull);
		Condition c12 = new Condition(t, in2, TransitionCondition.NotNull);
		Condition c13 = new Condition(t, out, TransitionCondition.IsNull);
		c11.SetNextCondition(LogicConnector.AND, c12);
		c12.SetNextCondition(LogicConnector.AND, c13);
		GuardMapping grd1 = new GuardMapping();
		grd1.condition = c11;
		ArrayList<PlaceNameWithWeight> inBoth = new ArrayList<>();
		inBoth.add(new PlaceNameWithWeight(in1, 1F));
		inBoth.add(new PlaceNameWithWeight(in2, 1F));
		grd1.Activations.add(new Activation(t, flrsAdd, inBoth, TransitionOperation.FLRS, output));
		t.GuardMappingList.add(grd1);

		// only in1
		Condition c21 = new Condition(t, in1, TransitionCondition.NotNull);
		Condition c22 = new Condition(t, in2, TransitionCondition.IsNull);
		Condition c23 = new Condition(t, out, TransitionCondition.IsNull);
		c21.SetNextCondition(LogicConnector.AND, c22);
		c22.SetNextCondition(LogicConnector.AND, c23);
		GuardMapping grd2 = new GuardMapping();
		grd2.condition = c21;
		ArrayList<PlaceNameWithWeight> inFirst = new ArrayList<>();
		inFirst.add(new PlaceNameWithWeight(in1, 1F));
		grd2.Activations.add(new Activation(t, flrsMove, inFirst, TransitionOperation.FLRS, output));
		t.GuardMappingList.add(grd2);

		// only in2
		Condition c31 = new Condition(t, in1, TransitionCondition.IsNull);
		Condition c32 = new Condition(t, in2, TransitionCondition.NotNull);
		Condition c33 = new Condition(t, out, TransitionCondition.IsNull);
		c31.SetNextCondition(LogicConnector.AND, c32);
		c32.SetNextCondition(LogicConnector.AND, c33);
		GuardMapping grd3 = new GuardMapping();
		grd3.condition = c31;
		ArrayList<PlaceNameWithWeight> inSecond = new ArrayList<>();
		inSecond.add(new PlaceNameWithWeight(in2, 1F));
		grd3.Activations.add(new Activation(t, flrsMove, inSecond, TransitionOperation.FLRS, output));
		t.GuardMappingList.add(grd3);

		t.Delay = 0;
		pn.Transitions.add(t);
	}

	// send next phase signals, move phase token
	private static void addPhaseTransition(PetriNet pn, String name, String fromPhase, String toPhase, int delay,
			String signalLanes13, String signalLanes24) {
		PetriTransition t = new PetriTransition(pn);
		t.TransitionName = name;
		t.InputPlaceName.add(fromPhase);

		GuardMapping grd = new GuardMapping();
		grd.condition = new Condition(t, fromPhase, TransitionCondition.NotNull);
		grd.Activations.add(new Activation(t, signalLanes13, TransitionOperation.SendOverNetwork, "TL1"));
		grd.Activations.add(new Activation(t, signalLanes24, TransitionOperation.SendOverNetwork, "TL2"));
		grd.Activations.add(new Activation(t, signalLanes13, TransitionOperation.SendOverNetwork, "TL3"));
		grd.Activations.add(new Activation(t, signalLanes24, TransitionOperation.SendOverNetwork, "TL4"));
		grd.Activations.add(new Activation(t, fromPhase, TransitionOperation.Move, toPhase));
		t.GuardMappingList.add(grd);

		t.Delay = delay;
		pn.Transitions.add(t);
	}
}

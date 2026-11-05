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
import DataOnly.PlaceNameWithWeight;
import DataOnly.TransferOperation;
import Enumerations.FZ;
import Enumerations.LogicConnector;
import Enumerations.TransitionCondition;
import Enumerations.TransitionOperation;

public class Lane3 {

	static final int LANE = 3;
	static final int PORT = 1083;

	// input flows
	static final float[] INPUT_FLOWS = { 0.4F, 0.35F, 0.3F, 0.25F, 0.2F, 0.3F, 0.4F, 0.5F, 0.45F, 0.4F,
			0.3F, 0.2F, 0.25F, 0.35F, 0.45F, 0.5F, 0.4F, 0.3F, 0.2F, 0.1F,
			0.2F, 0.3F, 0.4F, 0.3F, 0.2F, 0.25F, 0.35F, 0.45F, 0.4F, 0.3F };

	public static void main(String[] args) {
		PetriNet pn = createNet();

		System.out.println("Lane " + LANE + " started \n ------------------------------");
		pn.ShowLogInWindow = true;

		PetriNetWindow frame = new PetriNetWindow(false);
		frame.petriNet = pn;
		frame.setVisible(true);
	}

	public static PetriNet createNet() {
		PetriNet pn = new PetriNet();
		pn.PetriNetName = "Lane " + LANE;
		pn.NetworkPort = PORT;

		for (float flow : INPUT_FLOWS)
			pn.InputStrings.add("P_i:" + flow + "F");

		// FLRS Move
		FLRS flrsMove = new FLRS(new FV(FZ.NL), new FV(FZ.NM), new FV(FZ.ZR), new FV(FZ.PM), new FV(FZ.PL));
		flrsMove.Print();

		// Places
		DataFuzzy p_i = new DataFuzzy(); // input flows
		p_i.SetName("P_i");
		pn.PlaceList.add(p_i);

		DataFuzzy p_l = new DataFuzzy(); // lane
		p_l.SetName("P_l");
		pn.PlaceList.add(p_l);

		DataFuzzy p_l2 = new DataFuzzy(); // waits at the light
		p_l2.SetName("P_l2");
		pn.PlaceList.add(p_l2);

		// light from controller, starts Red
		pn.PlaceList.add(Intersection.signalPlace("P_TL", Intersection.RED));

		// to Intersection
		DataTransfer p_out = new DataTransfer();
		p_out.SetName("P_out");
		p_out.Value = new TransferOperation(Intersection.HOST, String.valueOf(Intersection.INTERSECTION_PORT),
				"P_i" + LANE);
		pn.PlaceList.add(p_out);

		// exit lane
		DataFuzzy p_o = new DataFuzzy(); // from Intersection
		p_o.SetName("P_o" + LANE);
		pn.PlaceList.add(p_o);

		DataFuzzy p_exit = new DataFuzzy();
		p_exit.SetName("P_Exit" + LANE);
		pn.PlaceList.add(p_exit);

		// Green <0,0,1,0,0>
		pn.ConstantPlaceList.add(Intersection.signalPlace("Green", Intersection.GREEN));

		// t_in
		PetriTransition t_in = new PetriTransition(pn);
		t_in.TransitionName = "t_in";
		t_in.InputPlaceName.add("P_i");

		Condition T_inCt1 = new Condition(t_in, "P_i", TransitionCondition.NotNull);
		Condition T_inCt2 = new Condition(t_in, "P_l", TransitionCondition.IsNull);
		T_inCt1.SetNextCondition(LogicConnector.AND, T_inCt2);

		GuardMapping grdT_in = new GuardMapping();
		grdT_in.condition = T_inCt1;

		ArrayList<PlaceNameWithWeight> inputIn = new ArrayList<>();
		inputIn.add(new PlaceNameWithWeight("P_i", 1F));
		ArrayList<String> outputIn = new ArrayList<>();
		outputIn.add("P_l");
		grdT_in.Activations.add(new Activation(t_in, flrsMove, inputIn, TransitionOperation.FLRS, outputIn));

		t_in.GuardMappingList.add(grdT_in);
		t_in.Delay = 0;
		pn.Transitions.add(t_in);

		// t_send
		PetriTransition t_send = new PetriTransition(pn);
		t_send.TransitionName = "t_send";
		t_send.InputPlaceName.add("P_l");

		Condition T_sendCt1 = new Condition(t_send, "P_l", TransitionCondition.NotNull);
		Condition T_sendCt2 = new Condition(t_send, "P_l2", TransitionCondition.IsNull);
		T_sendCt1.SetNextCondition(LogicConnector.AND, T_sendCt2);

		GuardMapping grdT_send = new GuardMapping();
		grdT_send.condition = T_sendCt1;

		ArrayList<PlaceNameWithWeight> inputSend = new ArrayList<>();
		inputSend.add(new PlaceNameWithWeight("P_l", 1F));
		ArrayList<String> outputSend = new ArrayList<>();
		outputSend.add("P_l2");
		grdT_send.Activations.add(new Activation(t_send, flrsMove, inputSend, TransitionOperation.FLRS, outputSend));

		t_send.GuardMappingList.add(grdT_send);
		t_send.Delay = Intersection.LANE_TRAVEL_TICKS; // travel time
		pn.Transitions.add(t_send);

		// t_out
		PetriTransition t_out = new PetriTransition(pn);
		t_out.TransitionName = "t_out";
		t_out.InputPlaceName.add("P_l2");
		t_out.InputPlaceName.add("P_TL");

		Condition T_outCt1 = new Condition(t_out, "P_l2", TransitionCondition.NotNull);
		Condition T_outCt2 = new Condition(t_out, "P_TL", TransitionCondition.Equal, "Green");
		T_outCt1.SetNextCondition(LogicConnector.AND, T_outCt2);

		GuardMapping grdT_out = new GuardMapping();
		grdT_out.condition = T_outCt1;
		grdT_out.Activations.add(new Activation(t_out, "P_l2", TransitionOperation.SendOverNetwork, "P_out"));
		grdT_out.Activations.add(new Activation(t_out, "P_TL", TransitionOperation.Move, "P_TL"));

		t_out.GuardMappingList.add(grdT_out);
		t_out.Delay = 0;
		pn.Transitions.add(t_out);

		// t_exit
		PetriTransition t_exit = new PetriTransition(pn);
		t_exit.TransitionName = "t_exit";
		t_exit.InputPlaceName.add("P_o" + LANE);

		GuardMapping grdT_exit = new GuardMapping();
		grdT_exit.condition = new Condition(t_exit, "P_o" + LANE, TransitionCondition.NotNull);

		ArrayList<PlaceNameWithWeight> inputExit = new ArrayList<>();
		inputExit.add(new PlaceNameWithWeight("P_o" + LANE, 1F));
		ArrayList<String> outputExit = new ArrayList<>();
		outputExit.add("P_Exit" + LANE);
		grdT_exit.Activations.add(new Activation(t_exit, flrsMove, inputExit, TransitionOperation.FLRS, outputExit));

		t_exit.GuardMappingList.add(grdT_exit);
		t_exit.Delay = 0;
		pn.Transitions.add(t_exit);

		pn.Delay = Intersection.LANE_TICK_MS;
		return pn;
	}
}

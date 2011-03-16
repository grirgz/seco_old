
s.boot
(
SynthDef("channel", {
	arg out, in, amp, bal;
	Out.ar(out, In.ar(in)*amp);

}).send(s);

SynthDef("sin1", {
	arg out, t_trig;
	Out.ar(out, In.ar(in)*amp);

}).send(s);
)
(

SynthDef(\bubblebub, {	|out=0, t_trig=1, attack=0.01, decay=0.08, pitchcurvelen=0.1, freq=1000, doneAction=0, amp=0.1|
	var pitch, son;
	amp   = amp * EnvGen.ar(Env.perc(attack, decay).delay(0.003), t_trig, doneAction: doneAction);
	pitch = freq * EnvGen.ar(Env.new([0,0,1],[0,1]).exprange(1, 2.718), t_trig, timeScale: pitchcurvelen);
	son = SinOsc.ar(pitch);
	// high-pass to remove any lowpitched artifacts, scale amplitude
	son = HPF.ar(son, 500) * amp * 10;
	Out.ar(out, son);
}).send(s);
)

(
	~keyboard = "azertyuiqsdfghjkwxcvbn,;";
	buttons = [];
	handlers = (0..(~numSteps*3)).collect ({ arg i;
		{
			buttons[i].value = ((buttons[i].value+1) % 2).postln;
			buttons[i].doAction.value;
		};
	});
	window.keyDownAction = { arg view, char, modifiers, u, k; 
		var f = ~keyboard.find(char.asString);
		f.postln;
		f.dump;
		handlers.postln;
		~keyboard.postln;
		buttons.postln;
		if ( f.isNil, {"plop".postln;}, {handlers[f].value});
	} ;
)
(
var width = 1110, height = 500;

// ============= helpers


~toggle_button = { arg button;
	button.value = ((button.value+1) % 2);
	button.doAction.value;
};
~toggle_value = { arg value;
	((value+1) % 2);
};
~mkdico = { arg col, f;
	var dico = Dictionary.new;
	var ret;
	col.collect { arg row, r;
		row.collect{ arg cell, c;
			ret = f.(row, r, cell, c);
			dico[ret[0]] = ret[1];
		};
		dico;
	};
};

// ============= raw inputs definitions

~kbpad8x4 = [
	[ 38, 233, 34, 39, 40, 45, 232, 95 ],
	[97, 122, 101, 114, 116, 121, 117, 105 ],
	[113, 115, 100, 102, 103, 104, 106, 107 ],
	[119, 120, 99, 118, 98, 110, 44, 59 ]
];
~cakewalk = (
	\knot: [
		74, 71, 65, 2, 5, 76, 77, 78, 10
	],
	\button: [
		21, 22, 23, 24, 25, 26, 27, 28
	],
	\toggle: [
		80,81,82,83
	],
	\slider: [
		73, 75, 72, 91, 92, 93, 94, 95, 7
	]
);

// ============= input symbol mapping

~midi = {
	var dico = Dictionary.new;
	~cakewalk.collect { arg v, k;
		"v:".postln;
		v.postln;
		"k:".postln;
		k.postln;
		v.do { arg raw, i;
			dico[raw] = (k.asString++i).asSymbol;
		};
	};
	dico;
}.value;
~cakewalk_bend2 = 1;
~cakewalk_master_knot = 0;

~numstep = 8;
~keyboard = { 
	var dico = Dictionary.new;
	~kbpad8x4.do { arg row, r;
		row.do {arg cell, c;
			dico[cell] = ("line"++r++"_step"++c).asSymbol
		};
	};
	dico

}.value;
~keyboard.value.postln;

// ============= gui

~make_window = {
	var window, buttons, handlers;
	var ul = [];
	window = GUI.window.new("seq", Rect(50, 50, width, height)).front;
	window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

	window;

};
~add_channel = {
	arg parent, name;
	var hl, b, txt, blist = [];
	hl = GUI.hLayoutView.new(parent, Rect(0,0,width+10,60));
	hl.background = Color.rand;
	txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
	txt.string = " " ++ name;

	~numstep.do { arg i;
		b = GUI.button.new(hl, Rect(50,50,50,50));
		i = i;
		b.states = [
			[ " " ++ i ++ " ", Color.black, Color.red],
			[ "=" ++ i ++ "=", Color.white, Color.black ]
		];
		blist = blist.add(b);

	};
	blist;
};


~stepsbut = [];
~win = ~make_window.value;
~stepsbut = ~stepsbut.add( ~add_channel.(~win, "chan1"));
~stepsbut = ~stepsbut.add( ~add_channel.(~win, "chan2"));

// ============= spec

~freqspec = ControlSpec(20,20000,\exp,1,440,"Hz");

// ============= data struct

~stepsamp = ~stepsbut.collect { arg row, r;
	row.collect{ arg cell, c;
		0	
	}
};
~stepsfreq = ~stepsbut.collect { arg row, r;
	//row.collect{ arg cell, c;
		300	
	//}
};
~currentline = 0;

// ============= handlers

~handlers = {
	var dico = Dictionary.new;
	~stepsbut.do { arg row, r;
		row.do { arg cell, c;
			dico[("line"++r++"_step"++c).asSymbol] = {
				//"==>>>".postln;
				//~stepsbut[r][c].postln;
				~toggle_button.(~stepsbut[r][c]);
				~stepsamp[r][c] = ~toggle_value.(~stepsamp[r][c]);
			}
		}
	};
	dico;

}.value;
~handlers.postln;
~midihandlers = {
	var dico = Dictionary.new;
	dico[\knot0] = { arg value;
		~stepsfreq[~currentline] = ~freqspec.map(value/127);
	};
	dico;
}.value;

// ============= main handlers

~win.view.keyDownAction = { arg view, char, modifiers, u, k; 
	//"{ ============".postln;
	~stepsbut.postln;
	~handlers.postln;
	if (~keyboard.at(u).notNil, { 
		//~keyboard[u].postln;
		//~handlers[~keyboard[u]].postln;
		~handlers[~keyboard[u]].value;
	});
	//"============ }".postln;
};
~ccresp = CCResponder({ |src,chan,num,value|
		"####============".postln;
		[src,chan,num,value].postln;
		~midi[num].postln;
		~midihandlers[~midi[num]].(value)
	},
	nil, // any source
	nil, // any channel
	nil, // any CC number
	nil // any value
);
~stepsamp.postln;

// ============= patterns connectors

~steps = { arg line;
	Prout({
		~stepsamp[line].do { arg a, i;
			//"NOW:".postln;
			//i.postln;
			//a.postln;
			//"-END".postln;
			a.yield;
		}
	});
};
~freqs = { arg line;
	Prout({
			"NOW:".postln;
		~stepsfreq[line].postln;
			"-END".postln;
		~stepsfreq[line].yield
	});
};
~legato = 0.1;

// ============= patterns 

~p1 = Pbind(

	//\degree, Pwhite(-7, 12, inf),
	\freq, Pn(~freqs.(0),inf),

	\dur, 0.5,
	\amp, Pn(~steps.(0),inf)*0.1,

	\legato, Pfunc({
		~stepsamp.postln;
		~legato;
	})

);

~p2 = Pbind(

	\instrument, \bubblebub,
	\degree, Pwhite(-7, 12, inf),
	\dur, 0.5,
	\amp, Pn(~steps.(1),inf)*0.1,

	\legato, Pfunc({~legato})

);

~whole = Ppar([
	~p2,
	~p1
]);
//~whole = ~p1;
~whole.play;


)

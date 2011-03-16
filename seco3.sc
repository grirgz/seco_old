s.boot
(
var	window, degreeView, ampView, width;

// use environment variable for parameters so that it persists

~parms = [
	degree: (spec: #[0, 7, \lin, 1, 0]),
	octave: (spec: #[2, 7, \lin, 1, 3]),
	amp: (spec: \amp),
	reAttack: (spec: #[0, 1, \lin, 1, 0]),
	filtAttack: (spec: #[0, 10, \lin])
];
~parm_list = [

	~parms 
];
~numSteps = 8;

width = 30 * ~numSteps;

~wind = GUI.window.new("seq", Rect(50, 50, (width + 20)*4, 140 * (~parms.size / 2))).front;
~wind.postln;
"======".postln;
~wind.view.decorator = FlowLayout(~wind.view.bounds); // notice that FlowView refers to w.view, not w
~make_channel = {
	arg parent;
	var window, buttons, handlers;

	~keyboard = "azertyuiqsdfghjkwxcvbn,;";
	buttons = [];
	handlers = (0..(~numSteps*3)).collect ({ arg i;
		{
			buttons[i].value = ((buttons[i].value+1) % 2).postln;
			buttons[i].doAction.value;
		};
	});
	window = GUI.compositeView.new(parent, Point(width+10, 640));
	window.background = Color.rand;
	window.decorator = FlowLayout(window.bounds);
	window.keyDownAction = { arg view, char, modifiers, u, k; 
		var f = ~keyboard.find(char.asString);
		f.postln;
		f.dump;
		handlers.postln;
		~keyboard.postln;
		buttons.postln;
		if ( f.isNil, {"plop".postln;}, {handlers[f].value});
	} ;
	// initialize parameters
	~parms.pairsDo({ |name, parm|
			// set up value array and convert spec
		parm.spec = parm.spec.asSpec;
		parm.val = parm.spec.default ! ~numSteps;
		"==============".postln;
		parm.val.postln;
		name.postln;

			// set up GUI
		parm.nameview = GUI.staticText.new(window, Rect(0, 0, width, 20)).string_(name);
		//~numSteps.do { GUI.button.new(window,Rect(0, 0, width/~numSteps, 100)).color.rand };
			if(name == \amp, {

				~numSteps.do { arg i;
					var col;
					var b = GUI.button.new( window, Rect( 0, 0 , width/~numSteps-4 , 24 ));
					col = if (i == (~numSteps / 2),  {
						Color.yellow
					}, if((i % 4) == 0, {
						Color.green
					}, {
						Color.white
					}));
					b.states = [
						[ " " ++ i ++ " ", Color.black, col],
						[ "=" ++ i ++ "=", Color.white, Color.black ]
					];
					buttons = buttons.add(b);
					b.action_({ |view|
						view.postln;
						"ACTUIN==============".postln;
						parm.val[i] = parm.spec.map(view.value);
					});

				};
			}, {

				GUI.multiSliderView.new(window, Rect(0, 0, width, 100))
					.thumbSize_(width / ~numSteps - 5)
					.gap_(5)
					.value_(parm.spec.unmap(parm.val))
					.action_({ |view|
						parm.val.overWrite(parm.spec.map(view.value), 0);
					});

			});
		GUI.staticText.new(window, Point(width, 8));	// no text, just a vertical spacer
	});
};
~make_channel.(~wind);
~make_channel.(~wind);

SynthDef(\bass, { |gate = 1, t_trig = 1, freq, freqlag = 0.1, ffreq = 1500, rq = 0.1, filtAttack = 0,
		amp = 1, out = 0|
	var	sig = Saw.ar(Lag.kr(freq, freqlag) * [1, 1.005]).sum,
		fenv = EnvGen.kr(Env([0, filtAttack, 0], [0.01, 0.2], -3), t_trig) + 1;
	sig = RLPF.ar(sig, ffreq * fenv, rq, amp)
		* EnvGen.kr(Env.adsr(0.01, 0.2, 0.5, 0.08), (gate > 0) - (t_trig > 0), doneAction: 2);
	Out.ar(out, sig ! 2);
}).send(s);
)

(
var	parmByName = { |name|
		var	i;
		if((i = ~parms.indexOf(name)).notNil) {
			~parms[i+1]
		};
	},
	ampStream, notePattern, restPattern, notePattern2;

// initialize streams for parameters
// supporting rests means that a stream may be called more than once at the same instant
// this use of Pclutch ensures that the stream advances only when the clock does
~parms.pairsDo({ |name, parm|
	parm.pstream = Pclutch(Pseq(parm.val, inf), Pdiff(Ptime(inf)) > 0).asStream;
});

ampStream = parmByName.(\amp).pstream;

notePattern = Pmono(
	\bass,	// synthdef name
	\amp, parmByName.(\amp).pstream,
	\degree, parmByName.(\degree).pstream,
	\octave, parmByName.(\octave).pstream,
	\filtAttack, parmByName.(\filtAttack).pstream,
	\trig, parmByName.(\reAttack).pstream,
	\dur, 0.25,
	\continue, Pif(Pkey(\amp) > 0, true, nil)
);
notePattern2 = Pmono(
	\bass,	// synthdef name
	\amp, parmByName.(\amp).pstream,
	\degree, parmByName.(\degree).pstream+4,
	\octave, parmByName.(\octave).pstream,
	\filtAttack, parmByName.(\filtAttack).pstream,
	\trig, parmByName.(\reAttack).pstream,
	\dur, 0.25,
	\continue, Pif(Pkey(\amp) > 0, true, nil)
);

restPattern = Pbind(
		// this Pbind must get a value from each parameter;
		// otherwise the streams will get out of sync
		// \freq --> \rest overrides \degree stream
	\freq, \rest,
	\degree, parmByName.(\degree).pstream,
	\octave, parmByName.(\octave).pstream,
	\filtAttack, parmByName.(\filtAttack).pstream,
	\trig, parmByName.(\reAttack).pstream,
	\amp, parmByName.(\amp).pstream,
	\dur, 0.25,
	\continue, Pif(Pkey(\amp) <= 0, true, nil)
);

~pattern = Prout({ |inval|
	var	amp;
	loop {
		if((amp = ampStream.next(inval)) > 0) {
			amp.postln;
			inval = notePattern2.embedInStream(inval);
		} {
			inval = restPattern.embedInStream(inval);
		};
	}
}).play(quant: 1);
~pattern = Prout({ |inval|
	var	amp;
	loop {
		if((amp = ampStream.next(inval)) > 0) {
			inval = notePattern.embedInStream(inval);
		} {
			inval = restPattern.embedInStream(inval);
		};
	}
}).play(quant: 1);
)
*/

~pattern.stop;


(

w=Window.new.front;

w.view.decorator = FlowLayout(w.view.bounds); // notice that FlowView refers to w.view, not w

v=CompositeView(w, Rect(5,5,190,390));

v.background = Color.rand; // set the color

v.decorator = FlowLayout(v.bounds);

y=CompositeView(w, Rect(205,5,190,390));

y.background = Color.rand; // set the color

y.decorator = FlowLayout(y.bounds);

14.do{ Slider(v, 180@20).background_(v.background) };// Points used, since the layout is handled by a decorator.

18.do{ Slider2D(y,58@58).background_(Color.rand); };

)

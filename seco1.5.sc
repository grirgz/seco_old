(
var	window, degreeView, ampView, width;

// use environment variable for parameters so that it persists
~numSteps = 16;
~parms = [
	degree: (spec: #[0, 7, \lin, 1, 0]),
	octave: (spec: #[2, 7, \lin, 1, 3]),
	amp: (spec: \amp),
	reAttack: (spec: #[0, 1, \lin, 1, 0]),
	filtAttack: (spec: #[0, 10, \lin])
];

width = 20 * ~numSteps;

~window = GUI.window.new("seq", Rect(50, 50, width + 20, 140 * (~parms.size / 2))).front;
~window.view.decorator = FlowLayout(Rect(0, 0, width + 20, 140 * ~parms.size), margin: Point(10, 10));

// initialize parameters
~parms.pairsDo({ |name, parm|
		// set up value array and convert spec
	parm.spec = parm.spec.asSpec;
	parm.val = parm.spec.default ! ~numSteps;

		// set up GUI
	parm.nameview = GUI.staticText.new(~window, Rect(0, 0, width, 20)).string_(name);
	GUI.multiSliderView.new(~window, Rect(0, 0, width, 100))
		.thumbSize_(width / ~numSteps - 5)
		.gap_(5)
		.value_(parm.spec.unmap(parm.val))
		.action_({ |view|
			parm.val.overWrite(parm.spec.map(view.value), 0);
		});
	GUI.staticText.new(~window, Rect(0, 0, width, 8));	// no text, just a vertical spacer
});

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
	ampStream, notePattern, restPattern;

// initialize streams for parameters
// supporting rests means that a stream may be called more than once at the same instant
// this use of Pclutch ensures that the stream advances only when the clock does
~parms.pairsDo({ |name, parm|
	parm.pstream = Pclutch(Pseq(parm.val, inf), Pdiff(Ptime(inf)) > 0).asStream;
});

ampStream = parmByName.(\amp).pstream;

notePattern = Pmono(
	\bass,	// synthdef name
	\degree, parmByName.(\degree).pstream,
	\octave, parmByName.(\octave).pstream,
	\filtAttack, parmByName.(\filtAttack).pstream,
	\trig, parmByName.(\reAttack).pstream,
	\amp, parmByName.(\amp).pstream,
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

~pattern = Proutine({ |inval|
	var	amp;
	loop {
		if((amp = ampStream.next(inval)) > 0) {
			inval = notePattern.embedInStream(inval)
		} {
			inval = restPattern.embedInStream(inval)
		};
	}
}).play(quant: 1);
)

~pattern.stop;

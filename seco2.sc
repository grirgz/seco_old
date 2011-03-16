// Also in my library is a mixing framework
m = MixerChannel(\test, s, 1, 2);
MixingBoard(\test, nil, m);

// target: m means to play the voicer through the mixer
v = MonoPortaVoicer(1, \bass, target: m);

(
var	parmByName = { |name|
		var	i;
		if((i = ~parms.indexOf(name)).notNil) {
			~parms[i+1]
		};
	};

~parms.pairsDo({ |name, parm|
	parm.pstream = Pseq(parm.val, inf);
});

~pattern = Pbind(
	\type, \voicerNote,
	\voicer, v,
	\amp, parmByName.(\amp).pstream,
	\degree, parmByName.(\degree).pstream,
	\degree, Pif(Pkey(\amp) > 0, Pkey(\degree), \rest),
	\octave, parmByName.(\octave).pstream,
	\filtAttack, parmByName.(\filtAttack).pstream,
	\trig, parmByName.(\reAttack).pstream,
	\dur, 0.25,
		// 1.1 means the current note will hold just longer than \dur
		// making a slide to the next note
		// normally 0 legato is not OK, but if the pitch is a rest
		// then there is no synth node to get stuck
	\legato, Pif(Pkey(\amp) > 0, 1.1, 0)
).play(quant: 1);
)

~pattern.stop;

Quarks.gui

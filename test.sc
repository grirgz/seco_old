(
~bla = (
	kb_handler: (),
	make_handlers: { arg self;
		var ps_list_size;
		self.kb_handler.debug("kb_handler4");
		self.kb_handler[ [0, ~kbfx[10]] ] = { self.handler( [\show_ve_panel] ) };
		self.kb_handler[ [0, ~kbfx[11]] ] = { self.handler( [\show_se_panel] ) };
		self.kb_handler.debug("kb_handler5");
		// map numeric keys to select playerset 
		ps_list_size = 2;
		ps_list_size.do { arg idx;
			idx.debug("niakkk");
			self.kb_handler[ \haha++idx ] = 4;
			self.kb_handler[\haha0].debug("hahahHAHA");
			self.kb_handler["haha0"].debug("hahahHAHAs");
			self.kb_handler.debug("kb_handler5.5").dump;
			//self.kb_handler[ [~modifiers[\ctrl], ~kbnumline[idx]] ] = 3;
			
			/*
			{
				"ps change".debug;
				self.handler( [\set_current_state, \playerset, self.model.playerset_list[idx] ] ); // main state
				self.init_state_from_playerset.value;
				//self.reload_current_panel.value;
				"haha";
			};
			*/
		};
		self.kb_handler.debug("kb_handler6");
		"handlers set".debug;

	}
);
~bla.make_handlers.value
)

(
~rah = Dictionary.new;
~rah["haha"] = 4;
~rah.debug("ha");
~rah["haha"].debug("ha");
~rah.at("haha").debug("ha");
)
(
~rah = ();
~rah[\haha] = 4;
~rah.debug("ha");
~rah[\haha].debug("ha");
~rah.at(\haha).debug("ha");
)

(
// select a chord and duration and repeat it for a random time interval
p = Pstep(
Pbind(
\ctranspose, Pseq([[0, 4, 7],[0, 7]],inf), 
\note, Pseq([0, 1,2,3],inf), 
\legato, 2.1,
\amp, 0.051,
\dur, 0.2
), 
2
//Prand([1, 2, 4], inf)/4
);
Ppar([p, p]).play;
)
// change degree independant of number of events that have been playing

(
Pchain(
Ppar([
Pbind(
\degree, Pbrown(0, 12, 1), 
\dur, Pstep( Pseq([0.1, 0.2, 0.4, 0.8, 1.6], inf), 3.2)
), 
Pbind(
\degree, Pbrown(0, 20, 1), 
\dur, Pstep( Pseq([0.1, 0.2, 0.4, 0.8, 1.6], inf), 4.5)
)
]), 
Pbind(
\scale, Pstep(Pseq([ [0, 2, 4, 5, 7, 9, 11], [0, 1, 2, 3, 4, 5, 6]], inf), 5), 
\db, Pstep(Pseq([2, -2, 0, -2], inf), 0.25) - 10
)
).play;

)

// use a simple pattern 
(
Pchain(
Ppar([
Pbind(
\octave, [5, 6] + Prand([0, 0, \r], inf), 
\degree, Proutine({ | ev | loop { ev = Pseq(ev[\degree]).embedInStream } }), 
\dur, Proutine({ loop { Pseq([0.2, 0.2, 0.2, 0.2, 0.3].scramble).embedInStream } })
), 
Pbind(
\octave, 4, 
\legato, 1.2, 
\dur, Proutine({ loop { Pseq([0.2, 0.2, 0.2, 0.2, 0.3].scramble * 5).embedInStream }})
), 
]), 
Pstep(Pbind(
\db, Pseq([0, -4, -2, -4, -3, -4, -3, -4], inf) - 20
), 0.2), 
Pstep(
Pbind(
\degree, Pfunc({ {10.rand}.dup(10) }), 
\scale, Pfunc({ {rrand(1, 2)}.dup(7).integrate })
), 
5
)
).play
)


// change one parameter
(
Pbind(
\degree, Pstep(Pseq([1, 2, 3, 4, 5]), 1.0).trace, 
\dur, Pseries(0.1, 0.1, 15)
).play;
)

// change degree independant of number of events that have been playing

(
var a, b;
a = Pbind(
\degree, Pstep(Pseq([0, 2b, 3], 1), 1.0), 
\dur, Prand([0.2, 0.5, 1.1, 0.25, 0.15], inf)
);
b = Pbind(
\degree, Pseq([0, 2b, 3], 1), 
\dur, 2, 
\ctranspose, -7
);
Pseq([Event.silent(1.25), Ppar([a, b])], inf).play;
)


// test tempo changes

(
var a, b;
a = Pbind(
\degree, Pstep(Pseq([0, 2b, 3], 1), 1.0), 
\dur, Prand([0.2, 0.5, 1.1, 0.25, 0.15], 9)
);
b = Pbind(
\degree, Pseq([0, 2b, 3], 1), 
\dur, 2, 
\ctranspose, -7
);

Ppar([a, b], inf).play;
)

SystemClock.sched(0, { TempoClock.default.tempo = [1, 2, 3, 5].choose.postln; 2 });

TempoClock.default.tempo = 1.0;


// timing test:
// parallel streams

(
var a, b, x;
var times, levels;

SynthDef(\pgrain, 
{ arg out = 0, freq=800, sustain=0.001, amp=0.5, pan = 0;
var window;
window = Env.sine(sustain, amp);
Out.ar(out, 
Pan2.ar(
SinOsc.ar(freq) * EnvGen.ar(window, doneAction:2), 
pan
) 
)
}
).store;

times = Pseq([3.4, 1, 0.2, 0.2, 0.2], inf);
levels = Pseq([0, 1, 2, 3, 4], inf);

a = Pstep(levels, times);
b = Pbind(\instrument, \pgrain, \octave, 7, \dur, 0.12, \degree, a);
x = times;

Ppar([b, Pset(\mtranspose, 2, b) ]).play;

b.play;
r {
var z = x.asStream; // direct times
0.5.wait;
loop {
z.next.wait;
s.makeBundle(0.2, {
Synth(\pgrain, [\freq, 3000, \sustain, 0.01]); // signal tone
})
}
}.play(quant:1)
)



(
Pdef(\instr1,Pbind(
	\instrument, \pgrain,
	\legato, Pseq([0.8, 0.3, 0.3, 0.3],inf)
))
)
(
Pdef(\instr1,Pbind(
	\instrument, \pgrain,
	\legato, 0.7
	//\legato, Pseq([0.8, 0.3, 0.3, 0.3],inf)
))
)

(
~arr = (
	bla: [5,0,7,0, 5,5,0,0]
);
)
(
~arr = (
	bla: [7,1,1,2, 2,2,3,5, 7,6,5]
);
)
~data = (
	v_snare1: (
		degree: [5,0,7,0, 5,5,0,0],
		stepline: [1,0,1,0,1,1,1,0]
	)
)

(
~pdynseq = { arg data, score, key, repeat = 1;
	Prout({
		var idx;
		repeat.do {
			idx = 0;
			while( { data[score][key][idx].notNil } , { 
				data[score][key][idx].yield;
				idx = idx + 1;
			});
		}
	})
}
)

~a = ~mkpindex.(\bla);
~a.next


(
Pdef(\score1, Pbind(
	\degree, ~pdynseq.(~data, \v_snare1, \degree, 4)
))
)
Pdef.defaultQuant = [8, 0, 0, 1]
Pdef.defaultQuant = 1
Pdef(\score1).quant = nil

Pdef(\player1, Pdef(\instr1) <> Pdef(\score1))

Pdef(\player1).play
Pdef(\instr1).play
Pdef(\score1).play

Pdef(\player1).stop
Pdef(\instr1).stop
Pdef(\score1).stop




s.boot




(
~iter_viewport = { arg board_size, viewport, fun;
	var abs_point, rel_point, board_rect, view_rect;

	board_rect = Rect.fromPoints( 0@0, board_size );
	view_rect = board_rect.sect(viewport);

	(view_rect.left..view_rect.right).do { arg rx, ax;
		(view_rect.top..view_rect.bottom).do { arg ry, ay;
			fun.( rx @ ry, ax @ ay );
		}
	}
}
)
~iter_viewport.value( 100@200, Rect.new(95,195, 10, 10), { arg rp, ap; [rp, ap].debug("rp, ap") } );
(5..10).do({ arg ry, ay; [ry, ay].debug("ry, ay");})


{RLPF.ar(Impulse.ar(SinOsc.kr(4,0,50,110)),Line.kr(400,3000,2,1,0,2),0.1)}.play 


s.boot
(
SynthDef("channel", {
	arg out, in, amp, bal;
	Out.ar(out, In.ar(in)*amp);

}).send(s);


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
~midi = {
	var dico = Dictionary.new;
	~cakewalk.collect { arg v, k;
		"v:".postln;
		v.postln;
		"k:".postln;
		k.postln;
		v.do { arg raw, i;
			dico[raw] = [k, i];
		};
	};
	dico;
}.value;
~mk_seqmachine = { arg y, x;

	var machine = Environment[
		\matrix -> (0 ! x ! y),
		\x -> x, // steps
		\y -> y  // rows
	];
	machine.know = true;
	machine;

};
~toggle_button = { arg button;
	button.value = ((button.value+1) % 2);
	button.doAction.value;
};
~toggle_value = { arg value;
	((value+1) % 2);
};

~mk_sequencer = { 
	var rows = 5, steps = 16;
	(

	// ============ Data

	rows: rows,
	steps: steps,
	boardstate: (
		focus: (
			current_channel: 0,
			current_synth: 0,
			current_part: 0
		),
		kbmod: \m4x8,
		sliders: [0,0,0,0, 0,0,0,0, 0],
		knots: [0,0,0,0, 0,0,0,0, 0],
		seqmachine: ~mk_seqmachine.(rows,steps)
			
	),
	channel: nil ! rows, // TODO: make it dynamic
	display_data: (
		stepbuttons: []
	),

	// ============ Helpers

	reader: (
		knot: { arg self, cursynthdat, key;
//			Prout({ cursynthdat[key] })
			Pfunc({ cursynthdat[key] })
		},
		stepline: { arg self, cursynthdat, key;
			Pn(Prout({ 
				cursynthdat[key].do ( _.yield )
			}))
		}

	),
	writer: (
		knot: { arg self, cursynth, type, curpart, index, value;
			var key;
			cursynth.debug("cursynth");
			key = cursynth[\argmap][type][curpart * 8 + index]; // get the synth arg corresponding to the numero of the modified knot (+ the part system)
			key.debug("key");
			cursynth[\data][key] = value;	
		},
		stepline: { arg self, cursynth, stepindex, value;
			cursynth[\data][\joue][stepindex] = ~toggle_value.(cursynth[\data][\joue][stepindex]) ;	


		}

	),
	display: (
		stepline: { arg self, dispdat, coor;
			var y = coor[1], x = coor[2];
			~toggle_button.(dispdat[\stepbuttons][y][x]);
		}

	),

	// player converter

	player_to_data: { arg self, player;
		var pat, data;
		player.debug("player_to_data:player");
		pat = player.synt.preset;
		data = Dictionary.new;
		player.synt.debug("player_to_data:player synth");
		player.synt.preset.debug("player_to_data:player preset");
		player.synt.argmap.debug("player_to_data:player argmap");
		player.synt.argmap.collect { arg v, k;

			"LOOP".postln;
			k.postln;
			v.postln;

			switch ( k,
				\knot , { 
					v.collect { arg synthkey;
						data[synthkey] = pat[synthkey];
						pat = Pbindf(pat, synthkey, self.reader.knot(data, synthkey));
					}
				},
				\stepline, {
					v.collect { arg stepkey;
						//data[stepkey] = self.boardstate.seqmachine;
						data[stepkey] = [1,1,1,1, 1,1,1,1]*0.1;
						pat = Pbindf(pat, stepkey, self.reader.stepline(data, stepkey));
					}
				}
			);
			
		};
		data[\joue] = 0 ! self.boardstate.seqmachine.x;
		pat = Pbindf(pat, \joue, self.reader.stepline(data, \joue));
		pat = Pbindf(pat, \type, Pif( Pkey(\joue) > 0 , \note, \rest)); // WTF with == ?????
		"ARG".postln;
		(
			pat:pat, 
			synt: [(
				argmap: player.synt.argmap,
				data: data
			)]
		);
	},


	// ============ Mapping

	kbmap: ( \m4x8: { 
		var dico = Dictionary.new;
		~kbpad8x4.do { arg row, r;
			row.do {arg cell, c;
				dico[cell] = [\step, r, c]
			};
		};
		dico

	}.value),

	// ============ Input handling

	handler: { arg self, input, value;
		var lib, cursynth;
		debug("================ handler");
		input.debug("input");
		cursynth = self.channel[self.boardstate.focus.current_channel][\synt][self.boardstate.focus.current_synth];
		lib = (
			knot: {
				self.writer.knot( cursynth, \knot, self.boardstate.focus.current_part, input[1], value);
			},
			midibut: {
				case
					{ input[1] < 4 } {
						self.boardstate.focus.current_synth = input[1];
					}
					{ input[1] >= 4 } {
						self.boardstate.focus.current_part = input[1];
					};
			},
			f: {
				self.boardstate.focus.current_channel = input[1];
			},
			char: {
				var map;
				map = self.kbmap[self.boardstate.kbmod][ input[1] ];  // char to symbol
				case
					{ map[0] == \step } {
						if( self.channel[map[1]].notNil, {
							cursynth = self.channel[map[1]][\synt][self.boardstate.focus.current_synth];
							//self.writer.stepline( cursynth, map[2], value); //called by button handler triggered in display helper
							self.display.stepline( self.display_data, map )
						})
					};

			},
			step: {
				var map = input;
				if( self.channel[map[1]].notNil, {
					cursynth = self.channel[map[1]][\synt][self.boardstate.focus.current_synth];
					self.writer.stepline( cursynth, map[2], value);
					//self.display.stepline( self.display_data, map )
				})
			}

		);
		lib[input[0]].value;
		cursynth.debug("------>> cursynth");
		debug("=======fin========= handler");

	},

	// ============ Methods

	jouer: { arg self, chan;
		if( self.channel[chan].play_handler.isNil, {
			self.channel[chan].play_handler = self.channel[chan].pat.play;
		});
	},
	taire: { arg self, chan;
		self.channel[chan].play_handler.stop;
		self.channel[chan].play_handler = nil;
	},
	set_chan: { arg self, chan, player;
		"=====set_chan".debug;	
		chan.debug("chan");
		player.debug("player");
		self.channel.put(chan, self.player_to_data(player));
		self.channel.debug("channel");
	}
	)
};

~mk_gui = { arg seq;
	var env = Environment.make({

		~width = 1110;
		~height = 500;
		~numstep = seq.boardstate.seqmachine.x;
		~stepsbut = [];

		~make_window = { arg self;
			var window, buttons, handlers;
			var ul = [];
			window = GUI.window.new("seq", Rect(50, 50, self.width, self.height)).front;
			window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

			window;

		};
		~add_channel = {
			arg self, parent, name;
			var hl, b, txt, blist = [];
			parent.debug("parenttttttttttt");
			hl = GUI.hLayoutView.new(parent, Rect(0,0,self.width+10,60));
			hl.background = Color.rand;
			txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
			txt.string = " " ++ name;

			self.numstep.do { arg i;
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

		~play = { arg self, chan;
			seq.jouer(chan)
		};
		~stop = { arg self, chan;
			seq.taire(chan)
		};

		~set_chan = { arg self, chan, player;
			seq.set_chan(chan, player)
		};

		~make_handlers = { arg self;

			self.win.view.keyDownAction = { arg view, char, modifiers, u, k; 
				seq.handler([\char, u, modifiers], 1)
			};
			self.ccresp = CCResponder({ |src,chan,num,value|
					"####============".postln;
					[src,chan,num,value].postln;
					~midi[num].postln;
					seq.handler(~midi[num])
				},
				nil, // any source
				nil, // any channel
				nil, // any CC number
				nil // any value
			);
		};

		~show = { arg self;
			self.win = self.make_window.value;
			self.win.debug("winnnnnnnnnnn");
			seq.boardstate.seqmachine.y.do { arg idx;
				var but;
				but = self.add_channel(self.win, "chan"++idx);
				but.do { arg bu, buidx;
					bu.action = { 
						[idx, buidx].debug("## button handler:");
						seq.handler([\step, idx, buidx]) 
						
					}
				};
				self.stepsbut = self.stepsbut.add( but);
			};
			seq.display_data[\stepbuttons] = self.stepsbut;
			self.make_handlers.value;
		}

	});
	env.know = true;
	env;
};

~mk_player = { arg freq; 
	(
		synt: (
			//spec: ~speclib.(\bubblebub),
			preset: (
				freq: freq,
				instrument: \bubblebub,
				dur: 0.5,
				amp: 0.05
			),
			argmap: (
				knot: [ \freq ]
			)
		)
	);
};

~seq = ~mk_sequencer.value;
~gui = ~mk_gui.(~seq);
[200, 300, 400, 500].do { arg fr, idx;
	~gui.set_chan( idx, ~mk_player.(fr) );
	~gui.play(idx)
};
~gui.show.value;
~gui.play(1)
//~w = ~gui.make_window.value
//~gui.add_channel.(~w, "plop")

)

(

~seq.handler( [\knot, 0], 500 );
~seq.handler( [\char, 233], 0 );
~seq.jouer(0);
~seq.taire(0);
//~a = ~seq.channel[0][\pat].asStream;
//~a.next(());
)
(

~player1 = (
	synt: (
		//spec: ~speclib.(\bubblebub),
		preset: (
			freq: 251,
			instrument: \bubblebub,
			dur: 0.5,
			amp: 0.1
		),
		argmap: (
			knot: [ \freq ]
		)
	)
);
~bla = (fre: 100);
~x = \type;
~a = Pbindf(~bla, 
	\freq, Pif(Pkey(\fre) == Pn(100), 500, 300));
~a.play;
~b = ~a.asStream;
~b.next(())

)
Pfunc({})


(

~rah = (
	bla: { arg this, xx;
		xx.debug("XX")
	},
	know: true,
	synt: (gah: 3)
);
~rah.synt.debug("nia");
~gah = ~rah.as(Environment);
~gah.know = true;
~gah.dump;
~gah.bla(1, 1, 1);



)
Dictionary[ 3 -> 4 ]
	/*
	channel: [ // list of channels
		( // channel 1
			pat: Pbind.new, // contains the pattern which will be played by the machine
			synt: [ // contains the list of gen and effects (synth without h because reserved method)
				( // generator 
					argmap: ( // map the synth arg to a type of control
						knot: [\freq, \fcut, \rq]
					)
					data: ( // hold the data. pat use a Reader to read it. The data is modified by Writers triggered by keyboard input
						freq: 440,
						fcut: 400,
						rq: 0.5,
						joue: [0,0,0,0, 0,0,0,0]
					)
				)
				// effect are not implemented yet
			],
			play_handler: nil
		)
	],
	*/

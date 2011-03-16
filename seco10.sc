s.boot
(
SynthDef("channel", {
	arg out, in, amp, bal;
	Out.ar(out, In.ar(in)*amp);

}).send(s);


SynthDef(\bubblebub, {	|out=0, t_trig=1, attack=0.01, decay=0.08, pitchcurvelen=0.1, freq=1000, doneAction=2, amp=0.1|
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
~kbfx = [
	// modifiers = 8388608
	63236, 63237, 63238, 63239, 63240, 63241, 63242, 63243, 63244, 63245, 63246, 63247
	//49,50,51,52,53,54,55,56,57,58
];
~kbarrow = (
	// modifiers = 8388608
	left: 63234,
	right: 63235,
	up: 63232,
	down: 63233,
);
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
};
~toggle_value = { arg value;
	((value+1) % 2);
};

~mk_sequencer = { 
	var rows = 10, steps = 16;
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
	gui_data: (),

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
				dico[cell] = [\step, r, c];
			};
		};
		dico;

	}.value),

	tid_by_cid: Dictionary.new,

	kb_handler: Dictionary.new,
	kb_mod_handler: Dictionary.new,
	//gui_handler: // handled by gui_data

	get_guibutton_by_id: { arg self, cid, sid;
		var tid = self.tid_by_cid[cid];
		self.gui_data[\track][tid][\step][sid];
	},




	display_handler: { arg self, ev;
		var cid = ev[1], sid = ev[2];
		~toggle_button.( self.get_guibutton_by_id(cid, sid) );
	},


	map_kbstepbuttons_to_chan: { arg self, kbrowidx, cid, startstep = 0;

		~kbpad8x4[kbrowidx].do { arg rawkc, idx;
			self.kb_handler[rawkc] = {
				var ev = [\step, cid, idx + startstep];
				self.handler( ev );
				ev.debug("end handler");
				self.display_handler( ev );
				ev.debug("end display_handler");
			}

		}	
		

	},

	map_guitrack_to_chan: { arg self, tid, cid, startstep=0;
		self.gui_data[\track][tid][\step].do { arg bt, idx;
			bt.action = {
				var ev = [\step, cid, idx + startstep];
				self.handler( ev );
			}

		}
		

	},

	// ============= gui making
	width: 1110,
	height: 500,

	make_guistepbutton: { arg self, parent, label, val=0;
		var bt;
		bt = GUI.button.new(parent, Rect(50,50,50,50));
		bt.states = [
			[ " " ++ label ++ " ", Color.black, Color.red],
			[ "=" ++ label ++ "=", Color.white, Color.black ]
		];
		bt.value = val;
		bt;
	},

	del_guitrack: { arg self, tid;
		self.gui_data[\track][tid][\step].do ( _.remove );
	},

	set_guitrack: { arg self, tid, cid;
		var bt, tr = [];
		self.del_guitrack(tid);
		self.channel[cid][\synt][0][\data][\joue].do { arg cell, idx;
			var hl, txt;
			hl = self.gui_data[\track][tid][\view];
			txt = self.gui_data[\track][tid][\label];
			txt.string = " Track " ++ cid;

			tr = tr.add( self.make_guistepbutton(hl, idx, cell) );

		};
		self.gui_data[\track][tid][\step] = tr;

	},

	make_window: { arg self;
		var window, buttons, handlers;
		var ul = [];
		window = GUI.window.new("seq", Rect(50, 50, self.width, self.height)).front;
		window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

		window;

	},

	map_kbfx_to_select_chan: { arg self;
		~kbfx.do { arg rawkc, idx;
			self.kb_handler[rawkc] = {
				self.handler( [\select_chan, idx] );
			};
		};
	},

	make_handlers: { arg self;

		self.window.view.keyDownAction = { arg view, char, modifiers, u, k; 
			u.debug("ooooooooooooo u");
			modifiers.debug("ooooooooooooo modifiers");
			self.kb_handler[u].value
		};
		self.ccresp = CCResponder({ |src,chan,num,value|
				"####============".postln;
				[src,chan,num,value].postln;
				~midi[num].postln;
				self.handler(~midi[num])
			},
			nil, // any source
			nil, // any channel
			nil, // any CC number
			nil // any value
		);
		self.map_kbfx_to_select_chan.value;

	},

	make_guibase: { arg self;
		var views = [], labels = [];
		self.gui_data[\track] = () ! 4;
		self.window = self.make_window.value;
		4.do { arg idx; // TODO make 4 dynamic
			var hl, txt;
			hl = GUI.hLayoutView.new(self.window, Rect(0,0,self.width+10,60));
			hl.background = Color.rand;
			txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
			txt.string = " BLANK " ++ idx;
			self.gui_data[\track][idx][\view] = hl;
			self.gui_data[\track][idx][\label] = txt;
		};
		self.make_handlers.value;
	},

	//map_midibuttons: // TODO

	// ============ handler callbacks

	

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
			select_chan: {
				var cid = input[1];
				cid.debug("select_chan==");
				4.do { arg idx; // TODO make 4 dynamic
					// TODO array overflow
					self.tid_by_cid[cid+idx] = idx;
					self.set_guitrack( idx, cid+idx );
					cid.debug("end set_guitrack");
					self.map_guitrack_to_chan( idx, cid+idx );
					cid.debug("end map_guitrack_to_chan");
					self.map_kbstepbuttons_to_chan( idx, cid+idx );
					cid.debug("end map_kbstepbuttons_to_chan");
				};
				self.window.view.focus(true);
				self.window.refresh;

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
	},
	select_chan: { arg self, cid;
		self.handler( [\select_chan, cid] );

	}
	);
};

// ================= PLAYING =========================

~mk_player = { arg freq; 
	(
		synt: (
			//spec: ~speclib.(\bubblebub),
			preset: (
				freq: freq,
				instrument: \bubblebub,
				dur: 0.05,
				doneAction: 2,
				pitchcurvelen: 0.5,
				amp: 0.05
			),
			argmap: (
				knot: [ \freq ]
			)
		)
	);
};

~seq = ~mk_sequencer.value;
[200, 300, 400, 500, 600, 700, 800].do { arg fr, idx;
	~seq.set_chan( idx, ~mk_player.(fr) );
	~seq.jouer(idx)
};
~seq.make_guibase.value;
~seq.select_chan(0);

)


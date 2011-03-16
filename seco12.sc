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
var make_widget_list_view, make_header_button, make_cell_button;

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
	down: 63233
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


// ============== Player lib

~player_lib = [
	\p_kick,
	\p_snare,
	\p_bub,
	\p_bla
];

// ============== GUI Helpers

make_widget_list_view = { arg parent, model_list, view_size, start_at, view_add, action_factory;
	var size;
	
	size = min(view_size, model_list.size - start_at);

	size.do { arg idx;
		var view_idx = idx + start_at;
		// view_add(parent, label, action )
		view_add.(parent, model_list[view_idx], action_factory.(view_idx) );
	};

};

make_header_button = { arg parent, label, action;
	var bt;

	bt = GUI.button.new(parent, Rect(50,50,50,50));
	bt.states = [
		[ " " ++ label ++ " ", Color.black, Color.red],
		[ "=" ++ label ++ "=", Color.white, Color.black ]
	];
	bt.value = 0;

	bt.action = action
};

make_cell_button = { arg parent, label, action;
	var bt;

	bt = GUI.button.new(parent, Rect(50,50,50,50));
	bt.states = [
		[ " " ++ label ++ " ", Color.black, Color.white],
		[ "=" ++ label ++ "=", Color.white, Color.black ]
	];
	bt.value = 0;

	bt.action = action
};

// =================================== 
// =================================== Make Sequencer
// =================================== 

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

	// change the pattern, called on input events
	writer: (
		knot: { arg self, pid, cursynth, type, curpart, index, value;
			var key;
			key = cursynth[\argmap][type][curpart * 8 + index]; // get the synth arg corresponding to the numero of the modified knot (+ the part system)
			Pbindef(pid, key, value);	
		},
		stepline: { arg self, psid, cursynth, stepindex, value;
			Pdefn(psid).source.list.debug("writer stepline");
			Pdefn(psid).source.list[stepindex] = ~toggle_value.(Pdefn(psid).source.list[stepindex]);
		}
	),
	display: (
		stepline: { arg self, dispdat, coor;
			var y = coor[1], x = coor[2];
			~toggle_button.(dispdat[\stepbuttons][y][x]);
		}

	),

	// player converter

	load_player: { arg self, player, cid;
		var pat, ppat, stepline, data, pid = self.pid(cid), psid = self.psid(cid);
		var pl_pid = pid++\pl, st_pid = pid++\step;
		player.debug("player_to_data:player");
		pat = player.synt.preset;

		player.synt.debug("player_to_data:player synth");
		player.synt.preset.debug("player_to_data:player preset");
		player.synt.argmap.debug("player_to_data:player argmap");
		pat.debug("================PAT");
		pat.patternpairs.debug("================PATp");

		//ppat = Pbindf(pat, \fausse, 1);

		//ppat.debug("================pPAT");

		Pdef(pl_pid, pat);

		stepline = 0 ! self.boardstate.seqmachine.x;
		Pdefn(psid, Pseq(stepline, inf));

		Pdef(st_pid, Pbind(\stepline, Pdefn(psid)));
		//Pbindef(pid, \stepline, Pdefn(psid));
		//Pbindef(pid, \instrument, \bubblebub);
		Pbindef(st_pid, \type, Pif( Pkey(\stepline) > 0 , \note, \rest)); // WTF with == ?????
		Pdef(pid, Pdef(pl_pid) <> Pdef(st_pid));

		self.channel[cid] = (
			synt: [(
				argmap: player.synt.argmap
			)]
		);
	},
	load_instr: { arg self, player, cid;
		var pat, ppat, stepline, data, pid = self.pid(cid), psid = self.psid(cid);
		player.debug("player_to_data:player");
		pat = player.synt.preset;

		player.synt.debug("player_to_data:player synth");
		player.synt.preset.debug("player_to_data:player preset");
		player.synt.argmap.debug("player_to_data:player argmap");
		pat.debug("================PAT");
		pat.patternpairs.debug("================PATp");

		//ppat = Pbindf(pat, \fausse, 1);

		//ppat.debug("================pPAT");

		//Pdef(pid, pat);
		//stepline = 0 ! self.boardstate.seqmachine.x;
		//Pdefn(psid, Pseq(stepline, inf));
		//Pbindef(pid, \stepline, Pdefn(psid));
		//Pbindef(pid, \instrument, \bubblebub);
		//Pbindef(pid, \type, Pif( Pkey(\stepline) > 0 , \note, \rest)); // WTF with == ?????
		Pbindef(pid, \freq, Pkey(\freq)*0.5); // WTF with == ?????

		self.channel[cid] = (
			synt: [(
				argmap: player.synt.argmap
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
		var bt, tr = [], psid = self.psid(cid);
		self.del_guitrack(tid);
		Pdefn(psid).source.list.do { arg cell, idx;
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


	//map_midibuttons: // TODO

	// ============ ve view


	model: (
		playerset_list: [
			\ps_perc,
			\ps_melo
		],
		playerset_dict: (
			ps_perc: [\p_kick, \p_snare],
			ps_melo: [\p_bub]
		),
		varset_dict: (
			ps_perc: [\vs_rock, \vs_reggae, 
				\vs_rock	,
				\vs_rock	,
				\vs_rock	,
				\vs_bla	,
				\vs_bla	
			],
			ps_melo: [\vs_sad, \vs_happy]
		),
		var_dict: (
			vs_rock: [\pd_kick1, \pd_snare1],
			vs_reggae: [\pd_kick2, \pd_snare2],
			vs_sad: [\pd_bub1],
			vs_happy: [\pd_bub2]
		)
	),

	// =================================== 
	// =================================== GUI Stuff
	// =================================== 

	make_pse_view: { arg self;
		var parent = self.window;
		var current_ps = \ps_perc;
		var ps_col_layout;
		var bt, label;
		var ps_col_size = 5;
		var size;
		var ps_col_model = self.model.playerset_dict[current_ps];
		var ps_col_bt_list = [];
		var start_at = 0;
		var vsm_size, vsm_row_size = 4, vsm_model = self.model.varset_dict[current_ps];
		var matrix_layout;



		// == add ps column

		ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/7,60*6));
		ps_col_layout.background = Color.rand;

		// add col header

		make_header_button.(ps_col_layout, current_ps, nil);
		"blaaaaaaaa".debug;

		// add ps list


		make_widget_list_view.(
			ps_col_layout,
			ps_col_model,
			ps_col_size,
			0,
			make_cell_button,
			{ arg idx;
				ps_col_model[idx].debug("=================argg");
				{ 
					ps_col_model[idx].debug("=========2========argg");
					// set cur player with ps name
					self.handler( [\set_current_player, ps_col_model[idx] ]  ) 
				}
			}
		);


		// == add players matrix

		matrix_layout = CompositeView(parent, Rect(0,0,(self.width-300),60*6));
		matrix_layout.decorator = FlowLayout(matrix_layout.bounds);
		matrix_layout.background = Color.rand;

		~player_lib.do { arg player;

			make_cell_button.(matrix_layout, player, {
				//self.handler( [\set
				"action!!".debug;
			})

		}


	},

	make_ve_view: { arg self;
		var parent = self.window;
		var current_ps = \ps_perc;
		var ps_col_layout;
		var bt, label;
		var ps_col_size = 5;
		var size;
		var ps_col_model = self.model.playerset_dict[current_ps];
		var ps_col_bt_list = [];
		var start_at = 0;
		var vsm_size, vsm_row_size = 4, vsm_model = self.model.varset_dict[current_ps];



		// == add ps column

		ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/7,60*6));
		ps_col_layout.background = Color.rand;

		// add col header

		make_header_button.(ps_col_layout, current_ps, nil);
		"blaaaaaaaa".debug;

		// add ps list


		make_widget_list_view.(
			ps_col_layout,
			ps_col_model,
			ps_col_size,
			0,
			make_cell_button,
			{ arg idx;
				ps_col_model[idx].debug("=================argg");
				{ 
					ps_col_model[idx].debug("=========2========argg");
					// set cur player with ps name
					self.handler( [\set_current_player, ps_col_model[idx] ]  ) 
				}
			}
		);


		// == add vs matrix

		vsm_size = min(vsm_row_size, vsm_model.size);

		vsm_size.do { arg idx;
			var cur_var_idx = idx+start_at;
			var current_vs = vsm_model[cur_var_idx];
			var vs_size, vs_model = self.model.var_dict[ current_vs ];
			var vs_col_layout;
			var bt, label;

			// == add vs col

			vs_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/7,60*6));
			vs_col_layout.background = Color.rand;

			// add vs header

			make_header_button.(vs_col_layout, current_vs, {});

			// add vs list
			make_widget_list_view.(
				vs_col_layout,
				vs_model,
				ps_col_size,
				0,
				make_cell_button,
				{ arg idx;
					{ self.handler( [\set_current_state, \var, vs_model[idx] ]  ) }// set cur player with ps name
				}
			);

		}
	},

	make_psle_view: { arg self;
		var parent = self.window;
		var psm_size = 5;
		var start_at = 0;
		var psm_model = self.model.playerset_list;



		// == add vs matrix

		psm_size = min(psm_size, psm_model.size);

		psm_size.do { arg idx;

			var ps_col_layout;
			var cur_ps_idx = idx+start_at;
			var current_ps = psm_model[cur_ps_idx];
			var ps_col_model = self.model.playerset_dict[current_ps];
			var ps_col_size = 5;

			// == add ps col

			ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/7,60*6));
			ps_col_layout.background = Color.rand;

			// add ps header

			make_header_button.(ps_col_layout, current_ps, nil);

			// add ps list
			make_widget_list_view.(
				ps_col_layout,
				ps_col_model,
				ps_col_size,
				0,
				make_cell_button,
				{ arg idx;
					{ 
						self.handler( [\set_current_state, \playerset, current_ps ]  ); // col
						self.handler( [\set_current_state, \player, self.model.playerset_dict[current_ps] ]  ); // cell
					}
				}
			);
		}
	},

	make_se_view: { arg self;
		var cv = \vs_rock;
		var start_at = 0;
		self.model.var_dict[cv].do { arg name, idx; // TODO make 4 dynamic
			var cur_player_idx = idx + start_at;
			var hl, txt;
			hl = GUI.hLayoutView.new(self.window, Rect(0,0,self.width+10,60));
			hl.background = Color.rand;
			txt = GUI.staticText.new(hl, Rect(0,0,100, 50));	// no text, just a vertical spacer
			txt.string = " " ++ cur_player_idx ++ ": " ++ name;
			self.gui_data[\track][idx][\view] = hl;
			self.gui_data[\track][idx][\label] = txt;
		};
	},

	show_se_panel: { arg self;
		self.clear_current_panel.value;
		self.boardstate.focus.panel = \se;
		self.make_se_view.value;
		self.make_se_handlers.value;
	},

	make_gui: { arg self;
		var views = [], labels = [];
		self.gui_data[\track] = () ! 4;
		self.window = self.make_window.value;
		self.show_ve_panel.value;
	},

	clear_current_panel: { arg self;
		self.window.view.removeAll.value;
		self.window.view.decorator = FlowLayout(self.window.view.bounds); // notice that FlowView refers to w.view, not w
	},

	show_psle_panel: { arg self;
		self.clear_current_panel.value;
		self.boardstate.focus.panel = \psle;
		self.make_psle_view.value;
		self.make_psle_handlers.value;
	},

	show_pse_panel: { arg self;
		self.clear_current_panel.value;
		self.boardstate.focus.panel = \psle;
		self.make_pse_view.value;
		self.make_pse_handlers.value;
	},

	show_ve_panel: { arg self;
		self.clear_current_panel.value;
		self.boardstate.focus.panel = \ve;
		self.make_ve_view.value;
		self.make_ve_handlers.value;
	},

	make_psle_handlers: { arg self;
		self.make_handlers.value;
	},
	make_pse_handlers: { arg self;
		self.make_handlers.value;
	},
	make_ve_handlers: { arg self;
		self.make_handlers.value;
	},

	// ============ handler callbacks

	

	// ============ Input handling

	handler: { arg self, input, value;
		var lib, cursynth, cid;
		debug("================ handler");
		input.debug("input");
		cid = self.boardstate.focus.current_channel;
		cursynth = self.channel[cid][\synt][self.boardstate.focus.current_synth];
		lib = (
			knot: {
				self.writer.knot(self.pid(cid), cursynth, \knot, self.boardstate.focus.current_part, input[1], value);
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

			// ========== var editor

			play_var: {
				var cv = self.boardstate.focus.state[\var];
				Pdef(cv).play;
			},
			stop_var: {
				var cv = self.boardstate.focus.state[\var];
				Pdef(cv).stop;
			},
			playstop_var: {
				var cv = self.boardstate.focus.state[\var];
				if( Pdef(cv).isPlaying, {
					Pdef(cv).stop;
				}, {
					Pdef(cv).start;
				})
			},

			play_varset: {
				var vs = self.boardstate.focus.state[\varset];
				self.model.var_dict[vs].do { arg va;
					Pdef(va).play
				}
			},
			stop_varset: {
				var vs = self.boardstate.focus.state[\varset];
				self.model.var_dict[vs].do { arg va;
					Pdef(va).stop
				}
			},

			set_current_state: {
				var statename, stateval;
				statename = input[1];
				stateval = input[2];

				self.boardstate.focus.state[statename] = stateval;

			},

			// ==========

			step: {
				var map = input;
				if( self.channel[map[1]].notNil, {
					self.channel[map[1]].debug("step handler");
					cursynth = self.channel[map[1]][\synt][self.boardstate.focus.current_synth];
					self.writer.stepline( self.psid(map[1]), cursynth, map[2], value);
					//self.display.stepline( self.display_data, map )
				})
			}


		);
		lib[input[0]].value;
		cursynth.debug("------>> cursynth");
		debug("=======fin========= handler");

	},

	// ============ Methods
	pat_prefix: \seq_,
	pat_stepline_prefix: \seq_stepline_,

	pid: { arg self, cid;
		(self.pat_prefix++cid).asSymbol
	},

	psid: { arg self, cid;
		var ret;
		cid.debug("psid");
		ret = (self.pat_stepline_prefix++cid).asSymbol;
		ret.debug("psid");
		ret;
	},

	pattern: { arg self, cid;
		Pdef(self.pid(cid))
	},

	jouer: { arg self, chan;
		self.pattern(chan).play;
	},
	taire: { arg self, chan;
		self.pattern(chan).stop;
	},
	set_chan: { arg self, chan, player;
		"=====set_chan".debug;	
		chan.debug("chan");
		player.debug("player");
		self.load_player(player, chan);
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
			preset: Pbind(
				\freq, freq,
				\instrument, \bubblebub,
				\dur, 0.15,
				\doneAction, 2,
				\pitchcurvelen, 0.5,
				\amp, 0.05
			),
			argmap: (
				knot: [ \freq ]
			)
		)
	);
};

~seq = ~mk_sequencer.value;


~seq.make_gui.value;
~seq.show_se_panel;
)
[200, 300, 400, 500, 600, 700, 800].do { arg fr, idx;
	~seq.set_chan( idx, ~mk_player.(fr) );
	//~seq.jouer(idx)
};
~seq.select_chan(0);
~seq.show_psle_panel;
~seq.show_ve_panel;
~seq.show_pse_panel;
~seq.show_se_panel;


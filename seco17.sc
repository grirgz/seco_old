s.boot;

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

~toggle_value = { arg value;
	if( value.isNil, { value = 0 });
	((value+1) % 2);
};

~toggle_button = { arg button;
	button.value = ((button.value+1) % 2);
};

~matrix3_from_list = { arg list, collectfun = { arg x; x };
	var banklist = List[], collist = List[], celllist = List[];
	var bankidx = 0, colidx = 0, cellidx = 0;
	list.do { arg asso;
		if( cellidx >= 4, {
			if( colidx >= 8, {
				banklist.add( collist );
				collist = List[];
				colidx = 0;
				bankidx = bankidx + 1;
			});
			collist.add( celllist );
			colidx = colidx + 1;
			cellidx = 0;
			celllist = List[];
		});
		celllist.add( collectfun.(asso) );
		cellidx = cellidx + 1;
	};
	banklist.add( collist );
	collist.add( celllist );
	banklist;

};

// ==========================================
// input keycode definition
// ==========================================

~kbpad8x4 = [
	[ 38, 233, 34, 39, 40, 45, 232, 95 ],
	[97, 122, 101, 114, 116, 121, 117, 105 ],
	[113, 115, 100, 102, 103, 104, 106, 107 ],
	[119, 120, 99, 118, 98, 110, 44, 59 ]
];
~kbnumline = [
	38, 233, 34, 39, 40, 45, 232, 95, 231, 224, 41, 61
];
~kbnumpad = [
	48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
];
~modifiers = (
	fx: 8388608,
	ctrl: 262144,
	shift: 131072,
	alt: 524288
);
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

// ==========================================
// 
// ==========================================

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

~pedynscalar = { arg data, key, repeat = inf;
	Prout({
		repeat.do {
			currentEnvironment[data][key].yield;
		};
	});
};

~make_player = { arg patfun;
	var get, data, nodeproxy;
	data = {
		var dict = Dictionary.new;
		patfun.argNames.do({ arg argName, idx;
			dict[argName] = patfun.defaultArgs[idx];
		});
		dict;
	};
	get = { arg argName; data[argName] };

	nodeproxy = NodeProxy.audio(s, 2);
	nodeproxy.source = patfun.valueArray( patfun.argNames.collect({ arg argName;
		switch(argName, 
			\stepline, { 
				var repeat = inf;
				Prout({
					var idx;
					repeat.do {
						idx = 0;
						while( { get.(argName)[idx].notNil } , { 
							get.(argName)[idx].yield;
							idx = idx + 1;
						});
					}
				})
			},
			\type, {
				Pif( Pkey(\stepline) > 0 , \note, \rest) // WTF with == ?????
			},
			//default:
			{
				Prout({
					var repeat = inf;
					repeat.do {
						get.(argName);
					}
				})
			}
		);
	}));

			

	(
		patfun: patfun,
		node: nodeproxy,
		data: data,
		get: get,
		set: { arg self, argName, val;
			data[argName] = val;
		},
		getargs: { arg self; patfun.argNames }
	)
};

~clone_player = { arg player;
	var newplayer;
	newplayer = ~make_player.(player.patfun);	
	newplayer.data = player.data;
};

~mk_sequencer = {(
	model: (
		boardsize: 10 @ 4,
		stepboardsize: 8 @ 4,

		patlib: [ // bank.y.x
			[
				[ \p_snare1, \p_kick1]
			]
		],
		parlive: [
			[
				[ \p_snare1_x1, 0, 0, 0],
				[ 0, 0, 0, 0],
				[ 0, 0, 0, 0]
			]
		]
			

	),
	state: (
		// TODO: implement offset
		selected: (
			coor: 0 @ 0,
			panel: \patlib,
			bank: 0,
			kind: \node // \node_from_lib, \node_from_parlivescreen, \nodegroup_from_parlivescreen
		),
		current: (
			panel: \patlib,
			offset: 0 @ 0,
			bank: 0
		),
		panel: (
			patlib: (
				bank: 0
			),
			parlive: (
				bank: 0
			)
		),
		clipboard: (
			node: \p_snare1,
			kind: \group // TODO:
		)
	),

	load_patlib: { arg self, patlist;
		var patpool, patlib, bank = 0, ix = 0, iy = 0;

		patlib = ~matrix3_from_list.(patlist, { arg asso;
			patpool[asso.key] = asso.value;
			asso.key;
		});
		self.model.patlib = patlib;
		self.model.patpool = patpool;
	},


	///////////////////////////////////////////////////////:::


	make_livenodename_from_libnodename: { arg self, name;
		// TODO: handle name collision
		"livenode_" ++ name;
	},

	make_newlivenodename_from_livenodename: { arg self, name;
		// TODO: make it real
		name ++ ".2";
	},

	make_livenode_from_libnode: { arg self, libnodename;
		var livenodename, livenodedict;
		livenodename = self.make_livenodename_from_libnodename(libnodename);
		self.model.livenodepool[livenodename] = ~make_player.(self.model.patlib[libnodename]);
		livenodename;
	},

	duplicate_livenode: { arg self, livenodename;
		var newlivenodename, newlivenode, newlivenode_pdict;
		newlivenodename = self.make_newlivenodename_from_livenodename(livenodename);
		self.model.livenodepool[newlivenodename] = ~clone_player.(self.model.livenodepool[livenodename]);
		newlivenodename;
	},

	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////// HANDLERS
	///////////////////////////////////////////////////////////////////////////////////////

	handlers: { arg self, input;
		input.debug("=====#############====== EVENT");
		switch( input[0],
			\select, {
				var coor = input[1];
				var oldsel = self.state.selected.deepCopy;

				if( self.state.current.panel == \parlive, {
					coor.y = coor.y + 1;
				});
				self.state.selected.coor = input[1];
				self.state.selected.panel = self.state.current.panel;
				self.state.selected.bank = self.state.current.bank;
				//self.debug("bah quoi");
				"bla".debug("nia");
				self.sync_button_state(oldsel);
				"bla".debug("bla");
				self.sync_button_state(self.state.selected);
				"bla".debug("rah");
			},
			\copy, {
				var sel = self.state.selected;
				self.state.clipboard.node = self.model[sel.panel][sel.bank][sel.coor.y][sel.coor.x];
			},
			\cut, {
				var sel = self.state.selected;
				self.state.clipboard.node = self.model[sel.panel][sel.bank][sel.coor.y][sel.coor.x];
				// TODO: forbid cut in patlib
				self.model[sel.panel][sel.bank][sel.coor.y][sel.coor.x] = 0;
				// TODO: refresh gui
			},
			\paste, {
				var sel = self.state.selected;
				// TODO: modify name when pasting from patlib to parlive
				switch( self.state.selected.kind, // TO
					\libnode, {
						"FORBIDEN".debug("paste to libnode");
					},
					\node, {
						switch( self.clipboard.kind, // FROM
							\libnode, {
								// create new livenode
								var name;
								var sel = self.state.selected;
								name = self.make_livenode_from_libnode(self.state.clipboard.node);
								self.model[sel.panel][sel.bank][sel.coor.y][sel.coor.x] = name;
								// TODO: refresh gui
							},
							\node, {
								// copy clipboard livenode and overwrite selected livenode
								// TODO: implement trashcan
							},
							\nodegroup, {
								// link nodegroup as livenode

							}
						);
					},
					\nodegroup, {
						switch( self.clipboard.kind,
							\libnode, {
								"FORBIDEN".debug("paste to libnode");
							},
							\node, {
								// append livenode to nodegroup
							},
							\nodegroup, {
								// copy clipboard nodegroup and overwrite selected nodegroup
								// TODO: implement trashcan
							}
						);
					}
				);
			},
			\change_panel, {
				var curpan = input[1];
				self.state.current.panel = curpan;
				"1".debug("FAUSSE");
				self.state.debug("FAUSSE");
				self.state.panel.debug("FAUSSE");
				self.state.panel[curpan].debug("FAUSSE");
				self.state.panel[curpan].bank.debug("FAUSSE");
				"7".debug("FAUSSE");
				self.state.current.bank = self.state.panel[curpan].bank;
				"8".debug("FAUSSE");
				self.refresh_current_panel;
				"6".debug("FAUSSE");
			},
			\change_bank, {
				var curpan = self.state.current.panel;
				"2".debug("FAUSSE");
				self.state.current.bank = input[1];
				"3".debug("FAUSSE");
				self.state.panel[curpan].bank = self.state.current.bank;
				"4".debug("FAUSSE");
				self.refresh_current_panel;
				"5".debug("FAUSSE");
			},

			\play_selected, {
				self.get_selected_player.node.play;
			},
			\stop_selected, {
				self.get_selected_player.node.stop;
			},
		)
	},


	make_kb_handlers: { arg self;

		self.kb_handler = Dictionary.new;

		self.kb_handler[ [~modifiers.fx, ~kbfx[4]] ] = { self.handlers( [\play_selected] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[5]] ] = { self.handlers( [\stop_selected] ) };

		self.kb_handler[ [~modifiers.fx, ~kbfx[8]] ] = { self.handlers( [\change_panel, \patlib] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[9]] ] = { self.handlers( [\change_panel, \parlive] ) };

		~kbpad8x4.do { arg line, iy;
			line.do { arg key, ix;
				self.kb_handler[[0, key]] = { self.handlers( [\select, ix @ iy] ) };
			}
		};

		~kbnumpad.do { arg keycode, idx;
			self.kb_handler[[0, keycode]] = { self.handlers( [\change_bank, idx] ) };
		};

		self.window.view.keyDownAction = { arg view, char, modifiers, u, k; 
			u.debug("ooooooooooooo u");
			modifiers.debug("ooooooooooooo modifiers");
			self.kb_handler[[modifiers,u]].value
		};


	},

	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////// GUI
	///////////////////////////////////////////////////////////////////////////////////////

	width: 1310,
	height: 500,

	make_cell_button: { arg self, parent, label, action;
		var bt;

		bt = GUI.button.new(parent, Rect(50,50,50,50));
		bt.states = [
			[ "  " ++ label ++ " ", Color.black, Color.white],
			[ "> " ++ label ++ " ", Color.black, Color.white],
			[ "  " ++ label ++ " ", Color.white, Color.black ],
			[ "> " ++ label ++ " ", Color.white, Color.black ],
		];
		bt.value = 0;

		bt.action = action
	},

	player_states: (
		\selection: [\selected, \deselected],
		\playing: [\stop, \play, \pause, \prepare_play, \prepare_stop, \prepare_resume],
		\clipboard: [\copy, \cut]
	),

	get_matrix_button: { arg self, point;
		self.window.view.children[point.x].children[point.y]
	},

	get_selected_name: { arg self;
		var sel = self.state.selected;
		self.model[sel.panel][sel.bank][sel.coor.x][sel.coor.y];
	},

	get_selected_player: { arg self;
		self.model.livenodepool[self.get_selected_name];
	},

	get_player_state: { arg self, address;
		var sel = self.state.selected, state = Dictionary.new;
		// selection
		if( (address.panel == sel.panel)
			&& (address.bank == sel.bank)
			&& (address.coor == sel.coor), {
			state[\selection] = \selected;
		}, {
			state[\selection] = \deselected;
		});
		// playing
		if( address.panel == \patlib, {
			state[\playing] = 	\stop;
		}, {
			try {
				if( self.livenodepool[ self.get_selected_name ].node.isPlaying, {
					state[\playing] = \play;
				}, {
					state[\playing] = \stop;
				});
			} {
				state[\playing] = \stop;
			};
		});
		//sel.debug("get_player_state sel");
		//address.debug("get_player_state address");
		//state.debug("get_player_state end state");
		state;
	},


	set_button_state: { arg self, button, state;
		state.debug("set_button_state");
		button.value = if( state[\selection] == \deselected, {
			if( state[\playing] == \stop, {
				0;
			}, {
				1
			});
		}, {
			if( state[\playing] == \stop, {
				2;
			}, {
				3
			});
		});
	},

	sync_button_state: { arg self, address;
		var state = self.get_player_state(address);
		// strange bug, must use intermediate var state else error occur
		//address.debug("set_button_state address");
		//state.debug("set_button_state state");
		self.set_button_state(self.get_matrix_button(address.coor), state)
	},

	make_parlive_view: { arg self; 
	
		var ps_col_layout, curbank, address;
		var parent = self.window;
		curbank = self.state.current.bank;

		8.do { arg rx;
			ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/9,60*8));
			ps_col_layout.background = Color.rand;

			8.do { arg ry;
				var label;

				ry.debug("maih QUOIIIII");
				label = try { 
					self.model.parlive[curbank][rx][ry];
				} {
					nil
				};
				label = switch(label,
					0, {"void"},
					nil, {"nil"},
					{ 
						label;
					}
				);
				self.make_cell_button(ps_col_layout, label, {  });
				address = (
					coor: rx @ ry,
					bank: curbank,
					panel: \parlive
				);
				self.sync_button_state(address);
			};

		};


	},

	make_patlib_view: { arg self;

		var ps_col_layout, curbank, address;
		var parent = self.window;
		curbank = self.state.current.bank;

		self.model.patlib[curbank].do { arg col, rx;
			ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/9,60*6));
			ps_col_layout.background = Color.rand;

			col.do { arg cell, ry;
				var label;
				label = cell;
				self.make_cell_button(ps_col_layout, label, {  });
				address = (
					coor: rx @ ry,
					bank: curbank,
					panel: \patlib
				);
				self.sync_button_state(address);
			};

		};

	},

	clear_current_panel: { arg self;
		self.window.view.removeAll.value;
		self.window.view.decorator = FlowLayout(self.window.view.bounds); // notice that FlowView refers to w.view, not w
		self.clear_kb_handler.value;
	},

	refresh_current_panel: { arg self;
		switch(self.state.current.panel,
			\parlive, { self.show_parlive_panel },
			\patlib, { self.show_patlib_panel }
		);

	},

	show_parlive_panel: { arg self;
		self.clear_current_panel.value;
		self.state.current.panel = \parlive;
		self.make_parlive_view;
		self.window.view.focus(true);
	},

	show_patlib_panel: { arg self;
		self.clear_current_panel.value;
		self.state.current.panel = \patlib;
		self.make_patlib_view;
		self.window.view.focus(true);
	},

	make_window: { arg self;
		var window, buttons, handlers;
		var ul = [];
		window = GUI.window.new("seq", Rect(50, 50, self.width, self.height)).front;
		window.view.decorator = FlowLayout(window.view.bounds); // notice that FlowView refers to w.view, not w

		window;

	},

	make_gui: { arg self;
		self.window = self.make_window.value;
		self.make_kb_handlers;
		self.show_patlib_panel;

	}


)};

~seq = ~mk_sequencer.value;
~seq.load_patlib( 140.collect({ arg idx; ("bla"++idx) -> {} }) );
~seq.make_gui;

)

~a = List[];
~a.add( [] )

(
	~matrix3_from_list = { arg list;
		var banklist = List[], collist = List[], celllist = List[];
		var bankidx = 0, colidx = 0, cellidx = 0;
		list.do { arg asso;
			if( cellidx >= 4, {
				if( colidx >= 8, {
					banklist.add( collist );
					collist = List[];
					colidx = 0;
					bankidx = bankidx + 1;
				});
				collist.add( celllist );
				colidx = colidx + 1;
				cellidx = 0;
				celllist = List[];
			});
			celllist.add( asso.key );
			cellidx = cellidx + 1;
		};
		banklist.add( collist );
		collist.add( celllist );
		banklist;

	};
	~res = ~load_patlib.( 242.collect({ arg idx; ("bla"++idx) -> {} }) );
	~res.do { arg bank, bankidx;
		bankidx.debug("==================== bank");
		bank.do { arg col, colidx;
			colidx.debug("col");
			col.do { arg cell;
				(cell++";  ").post;
			};
			"".postln;
		}
	};

)

0 == 8


(

~a = (
	nest: (
		bla: (
			niark: \haha
		),
		niark: (
			pouic: \hop
		)
	)
);

)


)



(

~a = (
	state: (
		// TODO: implement offset
		selected: (
			coor: 0 @ 0,
			panel: \patlib,
			bank: 0,
			kind: \node // \node_from_lib, \node_from_parlivescreen, \nodegroup_from_parlivescreen
		),
		current: (
			panel: \patlib,
			offset: 0 @ 0,
			bank: 0
		).asDictionary,
		panel: (
			patlib: (
				bank: 0
			),
			parlive: (
				bank: 0
			)
		),
		clipboard: (
			node: \p_snare1,
			kind: \group // TODO:
		)
	)
)

)

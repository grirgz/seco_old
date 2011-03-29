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
~kbcalphanum = {
	var dict = Dictionary.new;
	//TODO: only for Ctrl (262144) modifier, do others
	//NOTE: ^W close the window
	var keycodes = [
			[ 38, 233, 34, 39, 40, 45, 232, 31, 231, 224, 41, 61 ],
			[ 1, 26, 5, 18, 20, 25, 21, 9, 15, 16, 36 ],
			[ 17, 19, 4, 6, 7, 8, 10, 11, 12, 13, 249, 42], 
			[60, 24, 3, 22, 2, 14, 44, 59, 58, 33]
	];
	var alnum = [
		"1234567890)=",
		"azertyuiop^$",
		"qsdfghjklmù*",
		"<xcvbn,;:!"
	];
	keycodes.do { arg row, rowidx;	
		row.do { arg kc, kcidx;
			dict[ alnum[rowidx][kcidx].asString ] = kc;
		};
	};
	dict;
}.value;
~kb8x2line = [
	38, 97, 233, 122, 34, 101, 39, 114, 40, 166, 45, 121, 232, 117, 95, 105
];
~kbnumline = [
	38, 233, 34, 39, 40, 45, 232, 95, 231, 224, 41, 61
];
~kbnumpad = [
	48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
];
~numpad = (
	plus: 43,
	minus: 45
);
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
	\knob: [
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

~pedynscalar = { arg data, key, repeat = 100;
	Prout({
		repeat.do {
			currentEnvironment[data][key].yield;
		};
	});
};

// ==========================================
// PLAYER FACTORY
// ==========================================

~make_event_key_reader = { arg argName, self;
	switch(argName, 
		\stepline, { 
			var repeat = inf;
			Prout({
				var idx;
				repeat.do {
					idx = 0;
					while( { self.get_arg(argName)[idx].notNil } , { 
						self.get_arg(argName)[idx].debug("stepline yield");
						self.get_arg(argName).debug("step nil");
						idx.debug("step idx");
						self.get_arg(argName)[idx].yield;
						idx = idx + 1;
					});
					self.get_arg(argName).debug("step nil");
				}
			})
		},
		\type, {
			Pif( Pkey(\stepline) > 0 , \note, \rest) // WTF with == ?????
		},
		//default:
		{
			//self.data[argName] = PatternProxy.new;
			Prout({
				var repeat = inf;
				repeat.do {
					self.get_arg(argName).debug(argName++" yield");
					self.get_arg(argName).yield;
				}
			})
		}
	);
};

~player_get_arg = { arg self, argName;
	var ret;
	argName.dump;
	self.get_args.do { arg an; an.debug("an====").dump };
	ret = if(self.get_args.includes(argName), {
		if([\type, \stepline].includes(argName), {
			self.data[argName];
		}, {
			//self.data[argName].source;
			self.data[argName];
		})
	}, {
		("ERROR: player: no such arg: " ++ argName ++ "!" ++ self).postln;
		nil;
	});
	ret.debug("get_arg ret");
	ret;
};

~player_set_arg = { arg self, argName, val;
	if([\type, \stepline].includes(argName), {
		self.data[argName] = val;
	}, {
		//self.data[argName].source = val;
		self.data[argName] = val;
	})
};

~get_spec = { arg argName, defname=nil, default_spec=\freq;
	if( argName.asSpec.notNil, {
		argName.asSpec;
	}, {
		var spec = default_spec.asSpec;
		try { 
			spec = SynthDescLib.global.synthDescs[defname].metadata.specs[argName].asSpec
		};
		spec;
	});
};

~make_player_from_synthdef = { arg defname, data=nil;
	var player;
	var desc = SynthDescLib.global.synthDescs[defname];
	defname.debug("loading player from");
	desc.debug("synthDescs");
	player = (
		init: { arg self;

			self.data = {
					// use args and defaults values from synthdef to build data dict
					// if data dict given, deep copy it instead
					var dict;
					dict = Dictionary.new;
					if( data.isNil, {
						desc.controls.do({ arg control;
							dict[control.name.asSymbol] = control.defaultValue;
						});
					}, {
						dict = data.deepCopy;
					});
					dict;
			}.value;

			self.data[\dur] = self.data[\dur] ?? 0.5;
			self.data[\legato] = self.data[\legato] ?? 0.8;

			self.data[\stepline] = [1,1,1,1]; // TODO: make it dyn

			//TODO: handle t_trig arguments

			self.node.source = {
				var proxy = EventPatternProxy.new;
				var dict = Dictionary.new;
				var list = List[];
				self.data.keys.do { arg argName;
					dict[argName] = ~make_event_key_reader.(argName, self)
				};
				dict[\instrument] = defname;
				dict[\type] = ~make_event_key_reader.(\type, self);
				dict.debug("maked pbind dict");
				dict.pairsDo({ arg key, val; list.add(key); list.add(val)});
				Pbind(*list);
			}.value;
		},

		clone: { arg self;
			~make_player_from_synthdef.(defname, self.data);
		},
		map: { arg self, argName, val;
			~get_spec.(argName, defname).map(val);
		},
		unmap: { arg self, argName, val;
			~get_spec.(argName, defname).unmap(val);
		},

		get_args: { arg self;
			self.data.keys
		},
		node: EventPatternProxy.new,

		get_arg: ~player_get_arg,
		set_arg: ~player_set_arg
	)
};

~make_player_from_patfun = { arg patfun, data=nil;
	var player;
	player = (
		init: { arg self;

			self.data = {
					// use args and defaults values from synthdef to build data dict
					// if data dict given, deep copy it instead
					var dict;
					dict = Dictionary.new;
					if( data.isNil, {
						patfun.argNames.do({ arg argName, idx;
							dict[argName] = patfun.defaultArgs[idx];
						});
					}, {
						dict = data.deepCopy;
					});
					dict;
			}.value;

			self.node.source = patfun.valueArray( patfun.argNames.collect({ arg argName;
				~make_event_key_reader.(argName, self)
			}));
		},
		patfun: { arg self; patfun; },
		clone: { arg self;
			~make_player_from_patfun.(patfun, self.data);
		},
		map: { arg self, argName, val;
			// TODO: how to get synthdef spec
			~get_spec.(argName).map(val);
		},
		unmap: { arg self, argName, val;
			~get_spec.(argName).unmap(val);
		},
		node: EventPatternProxy.new,
		get_arg: ~player_get_arg,
		set_arg: ~player_set_arg
	);
	player.init;
	player;
};
Spec.add(\dur, ControlSpec(4/128, 4, \lin, 4/64, 0.25, "s"));
Spec.add(\legato, ControlSpec(0, 1.2, \lin, 0, 0.707));

~make_player = { arg instr, data=nil;
	var player = nil;
	case
		{ instr.isSymbol } {
			player = ~make_player_from_synthdef.(instr, data);
		} 
		{ instr.isFunction } {
			player = ~make_player_from_patfun.(instr, data);
		}
		{ ("ERROR: player type not recognized:"++instr).postln }
	;
	player.init;
	player;
};

// ==========================================
// SEQUENCER FACTORY
// ==========================================

~mk_sequencer = {(
	model: (
		boardsize: 10 @ 4,
		stepboardsize: 8 @ 4,

		livenodepool: Dictionary.new,
		patpool: Dictionary.new,

		patlib: [ // bank.y.x
			[
				[ \p_snare1, \p_kick1]
			]
		],


		get_parlive: { arg self, address;
			var ret;
			var path = address.bank @ address.coor.x;
			//address.debug("get_parlive address");
			try {
				ret = self.parlive[ path ][\data][address.coor.y];
				//self.parlive[ path ].debug("get_parlive path contenu");
				if( ret == 0, { ret = "void" });
			} {
				ret = "void";
			};
			//ret.debug("get_parlive ret");
			ret;
		},

		get_pargroup: { arg self, address;
			var path = address.bank @ address.coor.x;
			var ret;
			path.debug("get_pargroup path");
			ret = self.parlive[ path ];
			if( ret.isNil, {
				ret = (
					name: (\group ++ address.coor.x),
					data: [0,0,0,0, 0,0,0,0]
				)
			});
			ret;
		},

		set_parlive: { arg self, address, value;
			var path = address.bank @ address.coor.x;
			if( self.parlive.includesKey( path ), {
				self.parlive[ path ][\data][address.coor.y] = value;
			}, {
				var data = [0,0,0,0, 0,0,0,0];
				data[address.coor.y] = value;
				self.parlive[ path ] = (
					name: (\group ++ address.coor.x),
					data: data
				);
			})
		},

		parlive: Dictionary[
			0 @ 0 -> (name: \group0, data: [0,0,0,0, 0,0,0,0])
		]
			

	),
	state: (
		// TODO: implement offset
		get_cc: { arg self, ccid;
			// TODO: use whole ccid
			var ret = self.cc[ccid.number];
			if( ret.isNil, { 1 }, { ret });
		},
		set_cc: { arg self, ccid, val;
			// TODO: use whole ccid
			self.cc[ccid.number] = val;
		},
		cc: Dictionary.new,
		selected: (
			coor: 0 @ 1,
			panel: \parlive,
			bank: 0,
			kind: \node // \libnode, \node, \nodegroup
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
			editplayer: (
				bank: 0
			),
			parlive: (
				bank: 0
			)
		),
		clipboard: (
			node: \p_snare1,
			kind: \node // TODO:
		)
	),

	load_patlib: { arg self, patlist;
		var patpool, patlib, bank = 0, ix = 0, iy = 0;
		patpool = Dictionary.new;

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
		name++"_l"++UniqueID.next;

	},

	make_newlivenodename_from_livenodename: { arg self, name;
		// TODO: make it real
		name[ .. name.findBackwards("_l")  ] ++ "l" ++ UniqueID.next;
	},

	make_livenode_from_libnode: { arg self, libnodename;
		var livenodename;
		livenodename = self.make_livenodename_from_libnodename(libnodename);
		self.model.livenodepool[livenodename] = ~make_player.(self.model.patpool[libnodename]);
		livenodename;
	},

	duplicate_livenode: { arg self, livenodename;
		var newlivenodename, newlivenode, newlivenode_pdict;
		newlivenodename = self.make_newlivenodename_from_livenodename(livenodename);
		newlivenodename.debug("newlivenodename");
		livenodename.debug("livenodename");
		self.model.livenodepool.debug("livenodepool");
		self.model.livenodepool[newlivenodename] = self.model.livenodepool[livenodename].clone;
		newlivenodename;
	},

	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////// HANDLERS
	///////////////////////////////////////////////////////////////////////////////////////

	shift_address: { arg self, ad;
		var address = ad.deepCopy;
		address.coor.y = address.coor.y-1;
		address;
	},

	copy_selection: { arg self;
		var sel = self.state.selected;
		switch( sel.panel,
			\parlive, {
				self.state.clipboard.node = self.model.get_parlive(self.shift_address(sel));
				self.state.clipboard.kind = sel.kind;
			},
			\patlib, {
				self.state.clipboard.node = self.model.patlib[sel.bank][sel.coor.x][sel.coor.y];
				self.state.clipboard.kind = sel.kind;
			}
		);
	},

	remove_selection: { arg self;
		var sel = self.state.selected;
		switch( sel.panel,
			\parlive, {
				switch(sel.kind,
					\node, {
						self.model.set_parlive(sel, 0);
						self.refresh_button(self.get_matrix_button(sel.coor), "void");
						self.sync_button_state(sel);
					},
					\nodegroup, {
						"NOT_IMPLEMENTED".debug("remove nodegroup");
					}
				);
			},
			\patlib, {
				"FORBIDEN".debug("cut in libnode");
			}
		);


	},

	handlers: { arg self, input;
		input.debug("=====#############====== EVENT");
		switch( input[0],
			\select_libnode, {
				var libnode_name;
				var livenode_name;
				var source_coor = input[1];
				var source_bank = self.state.current.bank;
				var target_sel = self.state.selected.deepCopy;
				var sel = self.state.selected;

				self.state.selected.debug("select_libnode selected");

				libnode_name = self.model.patlib[source_bank][source_coor.x][source_coor.y];
				livenode_name = self.make_livenode_from_libnode(libnode_name);

				livenode_name.debug("new livenode name");

				target_sel.coor.y = target_sel.coor.y-1;
				self.model.set_parlive(target_sel, livenode_name);

				self.model.debug("MODEL");

				// back to parlive panel

				self.state.current.panel = \parlive;
				self.state.current.bank = self.state.panel[\parlive].bank;
				self.refresh_current_panel;

				self.refresh_button_at(sel, livenode_name);
				self.sync_button_state(sel);

			},
			\select, {
				var coor = input[1];
				var oldsel = self.state.selected.deepCopy;

				if( self.state.current.panel == \parlive, {
					coor.y = coor.y + 1;
				});
				self.state.selected.coor = input[1];
				self.state.selected.panel = self.state.current.panel;
				self.state.selected.bank = self.state.current.bank;
				self.state.selected.kind = switch( self.state.selected.panel,
					\patlib, { \libnode },
					\parlive, { \node }
				);
				//self.debug("bah quoi");
				self.state.selected.debug("selected");
				self.sync_button_state(oldsel);
				self.sync_button_state(self.state.selected);
			},
			\copy, {
				self.copy_selection;
			},
			\cut, {
				var sel = self.state.selected;
				self.copy_selection;
				self.remove_selection;
			},
			\paste, {
				var sel = self.state.selected;
				// TODO: modify name when pasting from patlib to parlive
				sel.debug("PASTE TO");
				self.state.clipboard.debug("PASTE FROM");
				switch( self.state.selected.kind, // TO
					\libnode, {
						"FORBIDEN".debug("paste to libnode");
					},
					\node, {
						switch( self.state.clipboard.kind, // FROM
							\libnode, {
								// create new livenode
								var name;
								var sel = self.state.selected;
								var address = self.state.selected.deepCopy;
								address.coor.y = address.coor.y-1;
								name = self.make_livenode_from_libnode(self.state.clipboard.node);
								name.debug("new node name");

								self.model.set_parlive(address, name);
								self.refresh_button_at(sel, name);
								self.sync_button_state(sel);
								self.model.debug("MODEL");
							},
							\node, {
								// copy clipboard livenode and overwrite selected livenode
								// TODO: implement trashcan

								var name;
								var sel = self.state.selected;
								var address = self.state.selected.deepCopy;
								address.coor.y = address.coor.y-1;
								name = self.duplicate_livenode(self.state.clipboard.node);
								name.debug("new node name");

								self.model.set_parlive(address, name);
								self.refresh_button_at(sel, name);
								self.sync_button_state(sel);
								self.model.debug("MODEL");
							},
							\nodegroup, {
								// link nodegroup as livenode

							}
						);
					},
					\nodegroup, {
						switch( self.state.clipboard.kind,
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
				var player = self.get_selected_player;
				if( player.isNil, { 
					"Dont play void player".debug("NONO");
				}, {
					
					player.node.dump;	
					player.node.source.dump;	
					player.node.play;	
					"fin play".debug;

				});
			},
			\stop_selected, {
				var player = self.get_selected_player;
				if( player.isNil, { 
					"Dont stop void player".debug("NONO");
				}, {
					player.node.stop;
				});
			}
		)
	},


	make_kb_handlers: { arg self;

		self.kb_handler = Dictionary.new;

		// Fx functions

		self.kb_handler[ [~modifiers.fx, ~kbfx[0]] ] = { self.handlers( [\copy] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[1]] ] = { self.handlers( [\cut] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[3]] ] = { self.handlers( [\paste] ) };

		self.kb_handler[ [~modifiers.fx, ~kbfx[4]] ] = { self.handlers( [\play_selected] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[5]] ] = { self.handlers( [\stop_selected] ) };

		self.kb_handler[ [~modifiers.fx, ~kbfx[8]] ] = { self.handlers( [\change_panel, \parlive] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[9]] ] = { self.handlers( [\change_panel, \patlib] ) };
		self.kb_handler[ [~modifiers.fx, ~kbfx[11]] ] = { self.handlers( [\change_panel, \editplayer] ) };

		// quant

		self.kb_handler[ [~modifiers.ctrl, ~kbcalphanum["q"]] ] = { 
			
			~kbnumpad.do { arg keycode, idx;
				self.kb_handler[[0, keycode]] = { 
					EventPatternProxy.defaultQuant = idx;

					// restore bank change shortcuts
					~kbnumpad.do { arg keycode, idx;
						self.kb_handler[[0, keycode]] = { self.handlers( [\change_bank, idx] ) };
					};

					("=== EventPatternProxy.defaultQuant changed to: " ++ EventPatternProxy.defaultQuant).postln;
				};
			};
			
		};



		self.window.view.keyDownAction = { arg view, char, modifiers, u, k; 
			u.debug("ooooooooooooo u");
			modifiers.debug("ooooooooooooo modifiers");
			self.kb_handler[[modifiers,u]].value
		};


	},


	make_parlive_handlers: { arg self;

		~kbpad8x4.do { arg line, iy;
			line.do { arg key, ix;
				self.kb_handler[[0, key]] = { self.handlers( [\select, ix @ iy] ) };
			}
		};

		~kbnumpad.do { arg keycode, idx;
			self.kb_handler[[0, keycode]] = { self.handlers( [\change_bank, idx] ) };
		};

	},

	make_patlib_handlers: { arg self;

		~kbpad8x4.do { arg line, iy;
			line.do { arg key, ix;
				self.kb_handler[[0, key]] = { self.handlers( [\select_libnode, ix @ iy] ) };
			}
		};

		~kbnumpad.do { arg keycode, idx;
			self.kb_handler[[0, keycode]] = { self.handlers( [\change_bank, idx] ) };
		};

	},
	
	make_editplayer_handlers: { arg self;


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

	refresh_button: { arg self, button, label;
		button.states = [
			[ "  " ++ label ++ " ", Color.black, Color.white],
			[ "> " ++ label ++ " ", Color.black, Color.white],
			[ "  " ++ label ++ " ", Color.white, Color.black ],
			[ "> " ++ label ++ " ", Color.white, Color.black ],
		];
	},

	refresh_button_at: { arg self, address, label;
		self.refresh_button(self.get_matrix_button(address.coor), label);
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
		var address = self.state.selected.deepCopy;
		var ret;
		address.coor.y = address.coor.y-1;
		//address.debug("get_selected_name address");
		//sel.debug("get_selected_name sel");
		switch( sel.panel,
			\parlive, {
				//address.debug("address");
				ret = self.model.get_parlive(address);
				//ret.debug("arf ret");
				//"hein".debug("bah oui");
			},
			\patlib, {
				//"gneeee".debug("bah oui");
				ret = self.model[sel.panel][sel.bank][sel.coor.x][sel.coor.y];
			}
		);
		//"quoi".debug("bah oui");
		//ret.debug("bah ret");
		ret;
	},

	get_selected_player: { arg self;
		var name = self.get_selected_name;
		name.debug("get_selected_player name");
		self.model.livenodepool[name];
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
		//state.debug("set_button_state");
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
			var label;

			ps_col_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10)/9,60*8));
			ps_col_layout.background = Color.rand;

			label = self.model.get_pargroup((bank: curbank, coor: rx @ 0))[\name];

			self.make_cell_button(ps_col_layout, label, {  });
			address = (
				coor: rx @ 0,
				bank: curbank,
				panel: \parlive
			);
			self.sync_button_state(address);

			8.do { arg ry;

				//ry.debug("maih QUOIIIII");
				label = self.model.get_parlive((coor: rx @ ry, bank: curbank));
				self.make_cell_button(ps_col_layout, label, {  });
				address = (
					coor: rx @ (ry+1),
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

	make_editplayer_view: { arg self; 
	
		var ps_row_layout;
		var main_layout;
		var make_midi_cc_widget;
		var make_stepline_widget;
		var make_player_model;
		var params = List[];
		var parent = self.window;

		var player = self.get_selected_player;

		params = player.get_args;
		params.debug("params");
		CCResponder.removeAll;

		make_midi_cc_widget = { arg parent, paramid, ccid=nil;
			var row_layout, ccname, param_val, param_value, cc_val, slider;
			var model;
			var parentself = self;
			row_layout = GUI.hLayoutView.new(parent, Rect(0,0,(self.width+10),30));
			row_layout.background = Color.rand;
			
			ccname = GUI.staticText.new(row_layout, Rect(0,0,60,30));
			ccname.string = "knob1";

			param_val = GUI.staticText.new(row_layout, Rect(0,0,60,30));
			param_value = player.get_arg(paramid);
			param_val.string = param_value;

			cc_val = GUI.staticText.new(row_layout, Rect(0,0,60,30));
			cc_val.string = player.map(paramid, parentself.state.get_cc(ccid));

			slider = GUI.slider.new(row_layout, Rect(0,0,60*6,30));
			slider.value = player.unmap(paramid, param_value);

			model = (
				blocked: \not,
				ccid: nil,
				set_value: { arg self, cc_value;
					var param_value;
					"quoi".debug("2");
					if(ccid.notNil, {
						"quoi".debug("3");
						cc_val.string = cc_value;
						self.unblock_do({
							"quoi".debug("4");
							param_value = cc_value;
							player.set_arg(paramid, param_value);
							"quoi".debug("5");
							param_val.string = param_value;
							"quoi".debug("6");
							slider.value = player.unmap(paramid, param_value);
						});
					}, {
						"quoi".debug("7");
						param_val.string = cc_value;
						"quoi".debug("8");
						slider.value = player.unmap(paramid, param_value);
					});
				},

				unblock_do: { arg self, fun;
					var cc_value = parentself.state.get_cc(ccid), param_value = player.get_arg(paramid) ;

					param_value.debug("unblock_do param_value");
					paramid.debug("unblock_do paramid");
					player.data.debug("unblock_do data");
					player.get_args.debug("unblock_do get_args");
					param_value = player.unmap(paramid, param_value);

					switch(self.blocked,
						\not, fun,
						\sup, {
							if( cc_value <= param_value , {
								self.blocked = \not;
								param_val.background = Color.green;
								fun.value;
							});
						},
						\inf, {
							if( cc_value >= param_value , {
								self.blocked = \not;
								param_val.background = Color.green;
								fun.value;
							});
						}
					);

				},
				
				block: { arg self;
					var cc_value = parentself.state.get_cc(self.ccid), param_value = player.get_arg(paramid) ;
					param_value = player.unmap(paramid, param_value);
					case 
						{ cc_value > param_value } {
							self.blocked = \sup;
							param_val.background = Color.red;
						}
						{ cc_value < param_value } {
							self.blocked = \inf;
							param_val.background = Color.red;
						}
						{ true } {
							self.blocked = \not;
							param_val.background = Color.green;
						}
				},

				assign_cc: { arg self, ccid;
					self.ccid = ccid;
					ccname.string = ccid.name;

					self.ccresp = CCResponder({ |src,chan,num,value|
							//[src,chan,num,value].debug("==============CCResponder");
							parentself.state.set_cc(ccid, value/127);
							self.set_value(player.map(paramid, value/127));
						},
						ccid.source, // any source
						ccid.channel, // any channel
						ccid.number, // any CC number
						ccid.val // any value
					);
				}

			);
			model.assign_cc(ccid);
			model.block;
			model;
		};

		make_stepline_widget = { arg parent, name;
			var row_layout, model, change_line;
			row_layout = GUI.hLayoutView.new(parent, Rect(0,0,(self.width+10),60));
			row_layout.background = Color.rand;

			name.debug("name");
			player.get_arg(name).debug("get param");

			change_line = { arg fun;
				var line, eline;
				line = player.get_arg(name);
				line.debug("change_line line");
				eline = fun.(line.copy);
				eline.debug("change_line eline");
				player.set_arg(name, eline);
			};
			model = (
				bar_length: 4,
				viewport: 0 @ 16,
				draw_buttons: { arg self;
					row_layout.removeAll;
					player.get_arg(name)[self.viewport.x .. self.viewport.y].do { arg step, stepidx;
						var bt;
						bt = make_cell_button.(row_layout, stepidx);
						if( step == 1, { ~toggle_button.(bt) });
					};
					row_layout.dump;
					row_layout.children.dump;
					row_layout.children[0].dump;
				},
				get_button: { arg self, idx;
					row_layout.children[idx];
				},
				toggle: { arg self, idx;
					idx.debug("toggle");	
					change_line.({ arg line;
						if( line[idx].notNil, {
							line[idx] = ~toggle_value.(line[idx]);
						});
						line;
					});
					if(self.get_button(idx).notNil, {
						~toggle_button.(self.get_button(idx));
					});
				},
				add_bar: { arg self, default=0;
					change_line.(_ ++ ( default ! self.bar_length ));
					self.draw_buttons;
				},
				del_bar: { arg self, default=0;
					change_line.({ arg line;
						var ll = line.size;
						line[ .. (line.size - self.bar_length -1 ) ] 
					});
					self.draw_buttons;
				},

				make_shortcuts: { arg self;
					var dict = Dictionary.new;
					dict[ [0, ~numpad[\plus]] ] = { self.add_bar };
					dict[ [~modifiers.ctrl, ~numpad[\plus]] ] = { self.del_bar };
					~kbpad8x4[0].do { arg kc, idx;
						dict[ [0, kc ] ] = { self.toggle(idx) };
					};
					dict;
				}

			);

			model.draw_buttons;
			self.kb_handler.putAll( model.make_shortcuts );
			model;
		};
		main_layout = GUI.vLayoutView.new(parent, Rect(0,0,(self.width+10),600));
		main_layout.background = Color.rand;

		// player line + effects(TODO) + amp

		ps_row_layout = GUI.hLayoutView.new(main_layout, Rect(0,0,(self.width+10),60));
		ps_row_layout.background = Color.rand;

		make_cell_button.(ps_row_layout, \amp, {  });

		make_midi_cc_widget.(ps_row_layout, \amp, (
			source: nil,
			channel: nil,
			number: ~cakewalk[\slider][0],
			name: "slider0",
			val: nil
		));

		// stepline

		ps_row_layout = GUI.hLayoutView.new(main_layout, Rect(0,0,(self.width+10),60));
		ps_row_layout.background = Color.rand;

		make_cell_button.(ps_row_layout, \stepline, {  });

		make_stepline_widget.(ps_row_layout, \stepline);

		// dur

		ps_row_layout = GUI.hLayoutView.new(main_layout, Rect(0,0,(self.width+10),60));
		ps_row_layout.background = Color.rand;

		make_cell_button.(ps_row_layout, \dur, {  });

		make_midi_cc_widget.(ps_row_layout, \dur, (
			source: nil,
			channel: nil,
			number: ~cakewalk[\slider][1],
			name: "slider1",
			val: nil
		));

		// others param
		params.reject([\stepline,\amp,\dur,\type,\out,\gate].includes(_)).do { arg param, knobidx;
			var ccid = nil;

			param.debug("parammmmm2");
			ps_row_layout = GUI.hLayoutView.new(main_layout, Rect(0,0,(self.width+10),30));
			ps_row_layout.background = Color.rand;

			param.debug("parammmmm3");
			make_cell_button.(ps_row_layout, param, {  });

			param.debug("parammmmm4");
			if(~cakewalk[\knob][knobidx].notNil, {
				ccid = (
					source: nil,
					channel: nil,
					number: ~cakewalk[\knob][knobidx],
					name: "knob" ++ knobidx,
					val: nil
				);
			});
			param.debug("parammmmm");
			make_midi_cc_widget.(ps_row_layout, param, ccid);
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
			\patlib, { self.show_patlib_panel },
			\editplayer, { self.show_editplayer_panel }
		);

	},

	show_parlive_panel: { arg self;
		self.clear_current_panel.value;
		self.state.current.panel = \parlive;
		self.make_parlive_view;
		self.make_parlive_handlers;
		self.window.view.focus(true);
	},

	show_patlib_panel: { arg self;
		self.clear_current_panel.value;
		self.state.current.panel = \patlib;
		self.make_patlib_view;
		self.make_patlib_handlers;
		self.window.view.focus(true);
	},

	show_editplayer_panel: { arg self;
		self.clear_current_panel.value;
		self.state.current.panel = \editplayer;
		self.make_editplayer_view;
		self.make_editplayer_handlers;
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
		self.show_parlive_panel;

	}


)};
)

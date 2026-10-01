"""Benchmark tracks. 'base' is the original JDK-only ReserveLedger; 't1-spring' is the same HTTP
contract on Spring Boot + JPA + Flyway + H2 with an offline Maven build."""
from __future__ import annotations
from dataclasses import dataclass
from pathlib import Path
from typing import Callable
from .runtime import ROOT

@dataclass(frozen=True)
class Track:
    name:str
    benchmark:str
    root:Path            # holds candidate/ and reference/
    trusted:tuple        # directories hashed into judge_sha256
    case_timeout:Callable[[str],int]
    def build(self,workspace,target):
        if self.name=='t1-spring':
            from . import spring;return spring.build(workspace,target)
        from .runtime import build;return build(workspace,target)
    def frozen_ok(self,workspace):
        if self.name=='t1-spring':
            from . import spring;return spring.frozen_ok(workspace)
        from .runtime import frozen_ok;return frozen_ok(workspace)
    def cases(self):
        if self.name=='t1-spring':
            from .cases_spring import CASES;return CASES
        from .cases import CASES;return CASES
    @property
    def mutation_module(self):return 'judge.spring_mutation'if self.name=='t1-spring'else 'judge.mutation'

TRACKS={
    'base':Track('base','ReserveLedger',ROOT,('judge','reference/src','reference/tests','candidate/src','candidate/docs'),
                 lambda cid:45 if cid.startswith('O')else 25),
    't1-spring':Track('t1-spring','ReserveLedger-Spring',ROOT/'tracks/t1-spring',
                 ('judge','tracks/t1-spring/reference/src','tracks/t1-spring/overlay','tracks/t1-spring/judge-tools','tracks/t1-spring/candidate'),
                 lambda cid:240 if cid.startswith('O')or cid in ('R03','R08','R09','R10','B08','B09')else 150),
}

def track(name):
    if name not in TRACKS:raise ValueError('unknown track: '+str(name))
    return TRACKS[name]

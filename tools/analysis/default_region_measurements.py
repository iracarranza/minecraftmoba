"""Canonical regional measurements even when no initial Homebase pair exists.

Diagnostic anchors only bypass the evaluator's early return. They never enter
compiler acceptance, saved Homebase evidence, or this independent metric vector.
"""
from __future__ import annotations
import hashlib
from pathlib import Path
from types import FunctionType

from vanilla_search import evaluate


def measure_without_home_pair(extracted, rotation, reflected):
    substitutions=[]
    original=evaluate._choose_home

    def diagnostic_home(rows,north,buildable,terrain_fit=False):
        picked=original(rows,north,buildable,terrain_fit=terrain_fit)
        if picked is not None:
            return picked
        substitutions.append('north' if north else 'south')
        # These placeholders are discarded. With fit_routes=False the 24
        # regional metrics do not read them; only Homebase bias/score do.
        return (0,len(rows[0])//2,round((len(rows)-1)*(.18 if north else .82)),0)

    diagnostic_globals={**evaluate.evaluate_orientation.__globals__, '_choose_home':diagnostic_home}
    diagnostic=FunctionType(evaluate.evaluate_orientation.__code__,diagnostic_globals,
                            'diagnostic_regional_measurements',evaluate.evaluate_orientation.__defaults__)
    result=diagnostic(extracted,rotation,reflected,fit_routes=False)
    metrics=dict(result['metrics'])
    metrics.pop('homeland_developability_bias',None)
    return {'metrics':metrics,'unavailable_metrics':['homeland_developability_bias'],
            'measurement_scope':'24 actual-surface regional measurements independent of Homebase availability',
            'provenance':{'method':'unchanged canonical evaluator formulas in a private diagnostic-only context',
                          'evaluator_source':str(Path(evaluate.__file__).resolve()),
                          'evaluator_sha256':hashlib.sha256(Path(evaluate.__file__).read_bytes()).hexdigest(),
                          'diagnostic_early_return_bypass_teams':substitutions,
                          'discarded_fields':['Homebases','Homebase bias','score','experimental acceptance','Routes'],
                          'acceptance_modified':False,'actual_source_surface_samples':True}}

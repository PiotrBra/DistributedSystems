from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict

app = FastAPI()


class Vote(BaseModel):
    text: str
    votes: int = 0


class Poll(BaseModel):
    id: int
    question: str
    choices: List[Vote]


polls: Dict[int, Poll] = {}
poll_counter = 1


@app.get("/polls/")
def get_polls():
    global polls
    return polls


@app.post("/polls/")
def create_poll(question: str, choices: List[str]):
    global poll_counter
    poll = Poll(id=poll_counter, question=question, choices=[Vote(text=choice) for choice in choices])
    polls[poll_counter] = poll
    poll_counter += 1
    return poll


@app.put("/polls/{id}")
def update_poll(id: int, question: str, choices: List[Vote]):
    global polls
    poll = Poll(id=id, question=question, choices=choices)
    polls[id] = poll
    return poll


@app.delete("/polls/{id]")
def delete_poll(id: int):
    global polls
    if polls[id]:
        del polls[id]
        return {"message": "Poll deleted successfully"}
    else:
        raise HTTPException(status_code=404, detail="Invalid poll id")


@app.get("/polls/{id}")
def get_poll(id: int):
    poll = polls.get(id)
    if not poll:
        raise HTTPException(status_code=404, detail="Poll not found")
    return poll


@app.post("/polls/{poll_id}/vote/{vote_index}")
def vote(poll_id: int, vote_index: int):
    poll = polls.get(poll_id)
    if not poll or vote_index >= len(poll.choices):
        raise HTTPException(status_code=404, detail="Invalid poll or vote index")
    poll.choices[vote_index].votes += 1
    return {"message": "Vote cast successfully"}


@app.get("/polls/{poll_id}/vote/{vote_index}")
def get_vote(poll_id: int, vote_index: int):
    poll = polls.get(poll_id)
    if not poll or vote_index >= len(poll.choices):
        raise HTTPException(status_code=404, detail="Invalid poll or vote index")
    else:
        return poll.choices[vote_index].votes


@app.get("/polls/{poll_id}/results")
def get_results(poll_id: int):
    poll = polls.get(poll_id)
    if not poll:
        raise HTTPException(status_code=404, detail="Poll not found")
    return {"question": poll.question, "results": {c.text: c.votes for c in poll.choices}}


@app.post("/polls/{poll_id}/vote{vote}")
def put_vote(poll_id: int, vote: str):
    poll = polls.get(poll_id)
    if not poll:
        raise HTTPException(status_code=404, detail="Poll not found")
    else:
        poll.choices.append(Vote(text=vote))
        return {"message": "Vote cast successfully"}


@app.put("/polls/{poll_id}/vote/{v}")
def put_result(poll_id: int, v: str):
    poll = polls.get(poll_id)
    if not poll:
        raise HTTPException(status_code=404, detail="Poll not found")
    for vote in poll.choices:
        if vote.text == v:
            if vote.votes > 0:
                vote.votes -= 1
                return {"message": "Vote updated successfully"}





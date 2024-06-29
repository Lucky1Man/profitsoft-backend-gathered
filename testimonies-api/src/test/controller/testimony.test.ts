import bodyParser from "body-parser";
import chai from "chai";
import chaiHttp from "chai-http";
import express from "express";
import mongoose from "mongoose";
import sinon from "sinon";
import { dateInputMiddleware } from "src/middlewares";
import Testimony from "src/model/testimony";
import routers from "src/routers/testimonies";
import * as executionFactService from "src/services/execution-fact";
import * as witnessService from "src/services/witness";
import mongoSetup from "../mongoSetup";

const { expect } = chai;

chai.use(chaiHttp);
chai.should();

const sandbox = sinon.createSandbox();

const app = express();

app.use(bodyParser.json({ limit: "1mb" }));

app.use(dateInputMiddleware);

app.use("/", routers);

// !!!!!!!!!!!! DO NOT CHANGE ORDER BECAUSE IT MATTERS IN TESTS !!!!!!!!!!!
const testTestimonies = [
  new Testimony({
    witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
    executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
    timestamp: "2004-01-01T00:00:00Z",
  }),
  new Testimony({
    witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
    executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
    timestamp: "2005-01-01T00:00:00Z",
  }),
  new Testimony({
    witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
    executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
    timestamp: "2006-01-01T00:00:00Z",
  }),
  new Testimony({
    witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
    executionFactId: "c93eec43-7d58-49e9-af1d-b77a40a13133",
    timestamp: "2006-01-01T00:00:00Z",
  }),
  new Testimony({
    witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
    executionFactId: "c93eec43-7d58-49e9-af1d-b77a40a13133",
    timestamp: "2006-01-01T00:00:00Z",
  }),
];

describe("Testimonies controller", () => {
  before(async () => {
    await mongoSetup;
  });

  const executionFactsServiceStub = sandbox.stub(
    executionFactService,
    "getExecutionFactById"
  );

  const witnessServiceStub = sandbox.stub(witnessService, "getWitnessById");

  afterEach(async () => {
    sandbox.resetHistory();
    await mongoose.connection.db.dropDatabase();
  });

  it("POST /testimonies must save given testimony", (done) => {
    const witnessId = "9fcb46b8-2d23-40d9-8b21-8678bacc563d";
    const executionFactId = "e32db94c-f2ca-4804-a7d8-90d35f65b57b";
    witnessServiceStub.withArgs(witnessId).resolves({ id: witnessId });
    executionFactsServiceStub
      .withArgs(executionFactId)
      .resolves({ id: executionFactId });
    chai
      .request(app)
      .post("")
      .send({
        witnessId: witnessId,
        executionFactId: executionFactId,
        timestamp: "2004-01-01T00:00:00Z",
      })
      .end(async (_, res) => {
        try {
          res.should.have.status(201);
          const inDb = await Testimony.findOne();
          expect(await Testimony.countDocuments()).to.equal(1);
          expect(inDb).to.exist;
          expect(inDb?._id.toString()).to.equal(res.body.id);
          expect(inDb?.executionFactId.toString()).to.equal(executionFactId);
          expect(inDb?.witnessId.toString()).to.equal(witnessId);
          done();
        } catch (ex) {
          done(ex);
        }
      });
  });

  [
    {
      data: {
        witnessId: "123",
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        timestamp: "2004-01-01T00:00:00Z",
      },
      expectedCode: 400,
      expectedMessage: "Given invalid witness id: 123.",
      beforeTestAction: () => {
        return;
      },
    },
    {
      data: {
        witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
        executionFactId: "123",
        timestamp: "2004-01-01T00:00:00Z",
      },
      expectedCode: 400,
      expectedMessage: "Given invalid execution fact id: 123.",
      beforeTestAction: (data: any) => {
        witnessServiceStub
          .withArgs(data.witnessId)
          .resolves({ id: data.witnessId });
      },
    },
    {
      data: {
        witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        timestamp: "9999-01-01T00:00:00Z",
      },
      expectedCode: 400,
      expectedMessage: "can not be in the future",
      beforeTestAction: () => {
        return;
      },
    },
    {
      data: {
        witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        timestamp: "2004-01-01T00:00:00Z",
      },
      expectedCode: 404,
      expectedMessage:
        "Given witness id: 9fcb46b8-2d23-40d9-8b21-8678bacc563d, does not exist.",
      beforeTestAction: (data: any) => {
        witnessServiceStub.withArgs(data.witnessId).resolves({ id: "" });
      },
    },
    {
      data: {
        witnessId: "9fcb46b8-2d23-40d9-8b21-8678bacc563d",
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        timestamp: "2004-01-01T00:00:00Z",
      },
      expectedCode: 404,
      expectedMessage:
        "Given execution fact id: e32db94c-f2ca-4804-a7d8-90d35f65b57b, does not exist.",
      beforeTestAction: (data: any) => {
        witnessServiceStub
          .withArgs(data.witnessId)
          .resolves({ id: data.witnessId });
        executionFactsServiceStub
          .withArgs(data.executionFactId)
          .resolves({ id: "" });
      },
    },
  ].forEach((testCase) => {
    it(`POST /testimonies must return error if invalid save dto: ${testCase.expectedMessage}`, (done) => {
      testCase.beforeTestAction(testCase.data);
      const witnessId = testCase.data.witnessId;
      const executionFactId = testCase.data.executionFactId;
      chai
        .request(app)
        .post("")
        .send({
          witnessId: witnessId,
          executionFactId: executionFactId,
          timestamp: testCase.data.timestamp,
        })
        .end(async (_, res) => {
          try {
            res.should.have.status(testCase.expectedCode);
            expect(res.body.message).to.include(testCase.expectedMessage);
            expect(await Testimony.countDocuments()).to.equal(0);
            done();
          } catch (ex) {
            done(ex);
          }
        });
    });
  });

  it("GET /testimonies must return data corresponding to filter", (done) => {
    const expected = testTestimonies.at(1);
    afterTestimoniesSaved(() => {
      const executionFactId = "e32db94c-f2ca-4804-a7d8-90d35f65b57b";
      chai
        .request(app)
        .get("")
        .query({
          executionFactId: executionFactId,
          size: 1,
          from: 1,
        })
        .end(async (_, res) => {
          try {
            expect(res.body.length).to.equal(1);
            const result = res.body.at(0);
            expect(result).to.exist;
            expect(result._id).to.equal(expected?._id.toString());
            expect(result.executionFactId).to.equal(expected?.executionFactId);
            expect(result.witnessId).to.equal(expected?.witnessId);
            expect(result.timestamp).to.equal(
              expected?.timestamp.toISOString()
            );
            expect(await Testimony.countDocuments()).to.equal(
              testTestimonies.length
            );
            done();
          } catch (ex) {
            done(ex);
          }
        });
    }, done);
  });

  const afterTestimoniesSaved = (
    callback: () => void,
    doneException: Mocha.Done
  ) => {
    Promise.all(
      testTestimonies.map((testimony) =>
        Testimony.findByIdAndUpdate(
          testimony._id,
          { $set: testimony },
          { new: true, upsert: true, runValidators: true }
        )
      )
    )
      .then(() => callback())
      .catch((ex) => doneException(ex));
  };

  [
    {
      expectedMessage: "Given invalid execution fact id: 123.",
      data: {
        executionFactId: "123",
        from: 0,
        size: 10,
      },
    },
    {
      expectedMessage: "From must be positive: -1.",
      data: {
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        from: -1,
        size: 10,
      },
    },
    {
      expectedMessage: "Size must be positive: -1.",
      data: {
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        from: 0,
        size: -1,
      },
    },
    {
      expectedMessage: "Size must be less then 300, given size: 301.",
      data: {
        executionFactId: "e32db94c-f2ca-4804-a7d8-90d35f65b57b",
        from: 0,
        size: 301,
      },
    },
    {
      expectedMessage: "Execution fact id query parameter is required.",
      data: {
        executionFactId: undefined,
        from: 0,
        size: 10,
      },
    },
  ].forEach((testCase) => {
    it(`GET /testimonies must return error if invalid filter: ${testCase.expectedMessage}`, (done) => {
      afterTestimoniesSaved(() => {
        const { executionFactId, size, from } = testCase.data;
        chai
          .request(app)
          .get("")
          .query({
            executionFactId: executionFactId,
            size: size,
            from: from,
          })
          .end(async (_, res) => {
            try {
              res.should.have.status(400);
              expect(res.body.message).to.include(testCase.expectedMessage);
              expect(await Testimony.countDocuments()).to.equal(
                testTestimonies.length
              );
              done();
            } catch (ex) {
              done(ex);
            }
          });
      }, done);
    });
  });

  it("POST /testimonies/_counts must return right response", (done) => {
    afterTestimoniesSaved(() => {
      const id1 = "eb4e82dd-9653-40f4-977c-903dc1dcd8d7"; //non existing id
      const id2 = testTestimonies.at(0)!.executionFactId;
      const id3 = testTestimonies.at(3)!.executionFactId;
      const id4 = "7963903f-1d20-4dcf-ae4c-31f1d2d6e3c6"; //non existing id
      const executionFactIds = [id1, id2, id3, id4];
      chai
        .request(app)
        .post("/_counts")
        .send({
          executionFactIds: executionFactIds,
        })
        .end(async (_, res) => {
          try {
            res.should.have.status(200);
            expect(res.body[id1]).to.exist;
            expect(res.body[id1]).to.equal(0);
            expect(res.body[id2]).to.exist;
            expect(res.body[id2]).to.equal(3);
            expect(res.body[id3]).to.exist;
            expect(res.body[id3]).to.equal(2);
            expect(res.body[id4]).to.exist;
            expect(res.body[id4]).to.equal(0);
            expect(await Testimony.countDocuments()).to.equal(
              testTestimonies.length
            );
            done();
          } catch (ex) {
            done(ex);
          }
        });
    }, done);
  });

  it("POST /testimonies/_counts must return error if one of given ids is invalid", (done) => {
    afterTestimoniesSaved(() => {
      const id1 = "eb4e82dd-9653-40f4-977c-903dc1dcd8d7"; //non existing id
      const id2 = "123";
      const id3 = testTestimonies.at(3)!.executionFactId;
      const id4 = "7963903f-1d20-4dcf-ae4c-31f1d2d6e3c6"; //non existing id
      const executionFactIds = [id1, id2, id3, id4];
      chai
        .request(app)
        .post("/_counts")
        .send({
          executionFactIds: executionFactIds,
        })
        .end(async (_, res) => {
          try {
            res.should.have.status(400);
            expect(res.body.message).to.include("Invalid id: 123, at position: 1.");
            expect(await Testimony.countDocuments()).to.equal(
              testTestimonies.length
            );
            done();
          } catch (ex) {
            done(ex);
          }
        });
    }, done);
  });
});
